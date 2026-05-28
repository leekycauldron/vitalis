package com.vitalis.assistant

import android.graphics.Bitmap
import android.util.Log
import com.vitalis.profile.PromptContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "Vitalis:AnthropicClient"
private const val MESSAGES_URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"
private const val HAIKU_MODEL = "claude-haiku-4-5-20251001"
private const val SONNET_MODEL = "claude-sonnet-4-6"

private val JSON = "application/json".toMediaType()

private const val MENU_CLASSIFIER_PROMPT =
    "You are a fast visual classifier. Decide whether the image clearly shows a restaurant menu: " +
        "a list of food or drink items, typically with names and prices, readable enough to make a " +
        "recommendation from. A plain table, food on a plate, or an unreadable blur is NOT a menu. " +
        "Respond with exactly one word: YES or NO. No punctuation, no explanation."

data class Recommendation(
    /** Cleanly-spelled dish name Sonnet read from the menu image (shown to the user). */
    val itemName: String,
    val reason: String,
    /**
     * Verbatim OCR line where this dish appears (possibly misspelled). Used to look up the
     * bounding box for the on-menu pulsating dot. Null if Sonnet didn't return one — caller
     * should fall back to fuzzy-matching [itemName].
     */
    val matchText: String?,
)

class AnthropicClient(private val apiKey: String) {

  private val client =
      OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(60, TimeUnit.SECONDS)
          .build()

  suspend fun isMenu(bitmap: Bitmap): Boolean =
      withContext(Dispatchers.IO) {
        val imageB64 = BitmapEncoding.toBase64Jpeg(bitmap, maxDim = 768, quality = 70)
        val body =
            buildMessagesBody(
                model = HAIKU_MODEL,
                maxTokens = 5,
                imageBase64 = imageB64,
                textPrompt = MENU_CLASSIFIER_PROMPT,
            )
        val responseText = postMessages(body) ?: return@withContext false
        val parsed = extractFirstText(responseText)?.trim()?.uppercase().orEmpty()
        val verdict = parsed.startsWith("YES")
        Log.d(TAG, "Haiku menu classifier -> '$parsed' (verdict=$verdict)")
        verdict
      }

  /**
   * Returns recommendations limited to items whose name matches one of [knownMenuItems] (OCR text).
   * Sonnet is told to pick items only from that allowlist, but we also filter after the fact.
   */
  suspend fun recommend(
      menuBitmap: Bitmap,
      context: PromptContext,
      knownMenuItems: List<String>,
      extraVoicePreference: String? = null,
  ): List<Recommendation> =
      withContext(Dispatchers.IO) {
        val imageB64 = BitmapEncoding.toBase64Jpeg(menuBitmap, maxDim = 1280, quality = 85)
        val prompt = buildRecommendPrompt(context, knownMenuItems, extraVoicePreference)
        val body =
            buildMessagesBody(
                model = SONNET_MODEL,
                maxTokens = 1024,
                imageBase64 = imageB64,
                textPrompt = prompt,
            )
        val responseText = postMessages(body) ?: return@withContext emptyList()
        val text = extractFirstText(responseText).orEmpty()
        parseRecommendations(text)
      }

  private fun buildRecommendPrompt(
      context: PromptContext,
      items: List<String>,
      extraVoicePreference: String?,
  ): String {
    val itemsBlock = items.joinToString(separator = "\n") { "- $it" }
    val voiceBlock =
        if (extraVoicePreference.isNullOrBlank()) "(none)"
        else extraVoicePreference
    return """
        You are a dietary recommendation assistant for a heads-up nutrition app called Vitalis.

        USER CONTEXT (profile, DNA notes, today's macros vs targets, current imbalances):
        ${context.toSystemBlurb()}

        VOICE PREFERENCE expressed by the user just now (e.g. "I'm in the mood for steak"):
        $voiceBlock

        You are seeing two things together: (1) a photo of the menu, and (2) an on-device OCR
        dump of every text line in that photo. The OCR is noisy — it routinely misspells words
        ("della" → "cella", "Margherita" → "Margerita") and includes non-dish lines (restaurant
        name, section headers like "STARTERS"/"Pasta"/"Drinks", descriptions, prices, page
        numbers, addresses).

        OCR LINES (verbatim, may be misspelled — use only to anchor a bounding box):
        $itemsBlock

        Read the actual dish names from the IMAGE (not the OCR text). Pick 1 to 3 SPECIFIC dishes
        or drinks that best fit ALL of: profile, DNA, today's macro deficits, and the voice
        preference (if any). Weight macro shortfall heavily — if protein is low, lean protein. If
        the user is over their carb target, deprioritise heavy carbs.

        For each pick return:
        - item_name: the dish/drink name spelled CORRECTLY (e.g. "Margherita Pizza",
          "Grilled Branzino", "Iced Matcha Latte"). Read from the image, fix OCR typos.
        - match_text: the exact OCR line above where this dish appears, copy-pasted verbatim
          INCLUDING any misspellings (e.g. "Margerita Pizza"). This is what we'll search for to
          place a marker on the menu. If the OCR dropped the dish entirely, return null.
        - reason: ONE sentence describing what the dish actually IS (key ingredients) AND why it
          fits the user's macro/profile need. Don't describe the restaurant in the abstract.

        HARD RULES:
        - item_name MUST be a single orderable dish or drink — NOT the restaurant name (e.g.
          "Osteria della Luna"), NOT a section header (e.g. "Starters", "Pasta", "From the
          Grill"), NOT a description, NOT a price.
        - If you cannot find any clearly orderable dishes in the image, return
          {"recommendations":[]}.

        Respond with a single JSON object, no prose, no code fences:
        {"recommendations":[{"item_name":"<clean name>","match_text":"<verbatim OCR line or null>","reason":"<one sentence>"}]}
        """.trimIndent()
  }

  private fun buildMessagesBody(
      model: String,
      maxTokens: Int,
      imageBase64: String,
      textPrompt: String,
  ): String {
    val source =
        JSONObject().apply {
          put("type", "base64")
          put("media_type", "image/jpeg")
          put("data", imageBase64)
        }
    val imagePart = JSONObject().apply {
      put("type", "image")
      put("source", source)
    }
    val textPart = JSONObject().apply {
      put("type", "text")
      put("text", textPrompt)
    }
    val content = JSONArray().apply {
      put(imagePart)
      put(textPart)
    }
    val message = JSONObject().apply {
      put("role", "user")
      put("content", content)
    }
    val messages = JSONArray().apply { put(message) }
    return JSONObject()
        .apply {
          put("model", model)
          put("max_tokens", maxTokens)
          put("messages", messages)
        }
        .toString()
  }

  private fun postMessages(body: String): String? {
    val req =
        Request.Builder()
            .url(MESSAGES_URL)
            .post(body.toRequestBody(JSON))
            .header("x-api-key", apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header("content-type", "application/json")
            .build()
    return try {
      client.newCall(req).execute().use { resp ->
        val text = resp.body?.string()
        if (!resp.isSuccessful) {
          Log.e(TAG, "Anthropic ${resp.code}: ${text?.take(500)}")
          null
        } else {
          text
        }
      }
    } catch (e: IOException) {
      Log.e(TAG, "Anthropic request failed", e)
      null
    }
  }

  private fun extractFirstText(responseJson: String): String? =
      try {
        val arr = JSONObject(responseJson).optJSONArray("content") ?: return null
        var result: String? = null
        for (i in 0 until arr.length()) {
          val block = arr.getJSONObject(i)
          if (block.optString("type") == "text") {
            result = block.optString("text").orEmpty()
            break
          }
        }
        result
      } catch (e: Exception) {
        Log.e(TAG, "Failed to parse Anthropic response", e)
        null
      }

  private fun parseRecommendations(rawText: String): List<Recommendation> {
    val jsonText = stripCodeFences(rawText).trim()
    return try {
      val obj = JSONObject(jsonText)
      val arr = obj.optJSONArray("recommendations") ?: return emptyList()
      buildList {
        for (i in 0 until arr.length()) {
          val rec = arr.getJSONObject(i)
          val name = rec.optString("item_name").trim()
          val reason = rec.optString("reason").trim()
          val matchText =
              rec.optString("match_text").trim().takeIf { it.isNotEmpty() && it != "null" }
          if (name.isNotEmpty()) add(Recommendation(name, reason, matchText))
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to parse recommendations JSON: ${jsonText.take(300)}", e)
      emptyList()
    }
  }

  private fun stripCodeFences(text: String): String {
    val trimmed = text.trim()
    if (!trimmed.startsWith("```")) return trimmed
    val withoutOpen = trimmed.removePrefix("```").let { s ->
      // strip optional language hint (e.g. ```json)
      val nl = s.indexOf('\n')
      if (nl >= 0) s.substring(nl + 1) else s
    }
    return withoutOpen.removeSuffix("```").trim()
  }
}

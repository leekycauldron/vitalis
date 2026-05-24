package com.vitalis.assistant

import android.graphics.Bitmap
import android.util.Log
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

data class Recommendation(val itemName: String, val reason: String)

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
      personalProfile: String,
      knownMenuItems: List<String>,
  ): List<Recommendation> =
      withContext(Dispatchers.IO) {
        val imageB64 = BitmapEncoding.toBase64Jpeg(menuBitmap, maxDim = 1280, quality = 85)
        val prompt = buildRecommendPrompt(personalProfile, knownMenuItems)
        val body =
            buildMessagesBody(
                model = SONNET_MODEL,
                maxTokens = 1024,
                imageBase64 = imageB64,
                textPrompt = prompt,
            )
        val responseText = postMessages(body) ?: return@withContext emptyList()
        val text = extractFirstText(responseText).orEmpty()
        val parsed = parseRecommendations(text)
        val allowedLower = knownMenuItems.map { it.lowercase() }.toSet()
        parsed.filter { rec -> rec.itemName.lowercase() in allowedLower || allowedLower.any { it.contains(rec.itemName.lowercase()) } }
      }

  private fun buildRecommendPrompt(profile: String, items: List<String>): String {
    val profileBlock = if (profile.isBlank()) "(no profile provided)" else profile
    val itemsBlock = items.joinToString(separator = "\n") { "- $it" }
    return """
        You are a dietary recommendation assistant for a heads-up nutrition app called Vitalis.

        User profile:
        $profileBlock

        OCR-extracted menu items (use these exact strings for item_name; do not invent items):
        $itemsBlock

        Pick 1 to 3 items from the list above that best fit the profile. Respond with a single JSON object,
        no prose, no code fences. Schema:
        {"recommendations":[{"item_name":"<exact string from list>","reason":"<one sentence>"}]}
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
          if (name.isNotEmpty()) add(Recommendation(name, reason))
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

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

private const val TAG = "Vitalis:RoastGenerator"
private const val MESSAGES_URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"
private const val MODEL = "claude-haiku-4-5-20251001"

private val JSON = "application/json".toMediaType()

class RoastGenerator(private val apiKey: String) {

  private val client =
      OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(20, TimeUnit.SECONDS)
          .build()

  /**
   * Generates a short, sarcastic roast for the user about the offending item. Returns null on
   * failure so the caller can skip TTS without breaking the food-log loop.
   */
  suspend fun generate(itemName: String, context: PromptContext, frame: Bitmap?): String? =
      withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
          Log.w(TAG, "Missing ANTHROPIC_API_KEY — skipping roast")
          return@withContext null
        }
        val body = buildBody(itemName, context, frame)
        val responseText = post(body) ?: return@withContext null
        extractFirstText(responseText)?.trim()?.takeIf { it.isNotEmpty() }
      }

  private fun buildBody(itemName: String, context: PromptContext, frame: Bitmap?): String {
    val prompt =
        """
        You are the user's witty, sarcastic friend who keeps them honest about their eating goals.
        You just spotted them about to eat $itemName. Write ONE short roast (1-2 sentences, max
        ~30 words) that will make them put it down. Be playful, specific to what is in the image
        and to their CURRENT macro state, and a little dramatic. If their carbs/fat are running
        hot or their protein is low, riff on that specifically. Do NOT be cruel, preachy, or use
        slurs. No emojis. No quotation marks. Output only the roast text, nothing else.

        USER CONTEXT:
        ${context.toSystemBlurb()}
        """.trimIndent()

    val content = JSONArray()
    if (frame != null) {
      val source =
          JSONObject().apply {
            put("type", "base64")
            put("media_type", "image/jpeg")
            put("data", BitmapEncoding.toBase64Jpeg(frame, maxDim = 768, quality = 70))
          }
      content.put(JSONObject().apply {
        put("type", "image")
        put("source", source)
      })
    }
    content.put(JSONObject().apply {
      put("type", "text")
      put("text", prompt)
    })

    val message =
        JSONObject().apply {
          put("role", "user")
          put("content", content)
        }

    return JSONObject()
        .apply {
          put("model", MODEL)
          put("max_tokens", 120)
          put("messages", JSONArray().put(message))
        }
        .toString()
  }

  private fun post(body: String): String? {
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
        } else text
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
        Log.e(TAG, "Failed to parse roast response", e)
        null
      }
}

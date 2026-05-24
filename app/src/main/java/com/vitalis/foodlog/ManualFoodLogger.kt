package com.vitalis.foodlog

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

private const val TAG = "Vitalis:ManualFoodLogger"
private const val URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"
private const val MODEL = "claude-haiku-4-5-20251001"
private const val TOOL_NAME = "log_manual_food"

private val JSON = "application/json".toMediaType()

/**
 * Parses a single free-text food entry (e.g. "1 medium apple", "2 slices pepperoni pizza") into a
 * single [FoodDetection] using Haiku tool-use. Caller decides whether to persist via
 * [FoodLogRepository.tryLog].
 */
class ManualFoodLogger(private val apiKey: String, private val knownFoods: Map<String, FoodInfo>) {

  private val client =
      OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .build()

  suspend fun parse(text: String): FoodDetection? =
      withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || text.isBlank()) {
          Log.w(TAG, "Missing key or empty text")
          return@withContext null
        }
        val body = buildBody(text)
        val raw = post(body) ?: return@withContext null
        parseDetection(raw)
      }

  private fun buildBody(userText: String): String {
    val known =
        knownFoods.keys.sorted().joinToString(", ").ifBlank { "(none — estimate everything)" }
    val prompt =
        """
        The user is logging a food they just ate, described in plain language. Estimate macros for
        the described amount (not a default serving — respect the quantity they wrote).

        Call $TOOL_NAME exactly once. For label, copy one of KNOWN LABELS if it matches exactly,
        otherwise null. The name should be a short, canonical lowercase noun reflecting what they
        actually ate. is_junk: true if it's mostly added sugar / refined carbs / deep-fried.

        KNOWN LABELS: $known

        USER TEXT: ${userText.trim()}
        """.trimIndent()

    val tool =
        JSONObject().apply {
          put("name", TOOL_NAME)
          put("description", "Log a single manually-entered food item with estimated macros.")
          put(
              "input_schema",
              JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject().apply {
                      put("label", JSONObject().apply { put("type", JSONArray().put("string").put("null")) })
                      put("name", JSONObject().apply { put("type", "string") })
                      put("calories", JSONObject().apply { put("type", "number") })
                      put("protein_g", JSONObject().apply { put("type", "number") })
                      put("carbs_g", JSONObject().apply { put("type", "number") })
                      put("fat_g", JSONObject().apply { put("type", "number") })
                      put("is_junk", JSONObject().apply { put("type", "boolean") })
                    },
                )
                put(
                    "required",
                    JSONArray()
                        .put("label").put("name").put("calories")
                        .put("protein_g").put("carbs_g").put("fat_g").put("is_junk"),
                )
              },
          )
        }

    val message =
        JSONObject().apply {
          put("role", "user")
          put(
              "content",
              JSONArray().put(JSONObject().apply {
                put("type", "text")
                put("text", prompt)
              }),
          )
        }

    return JSONObject()
        .apply {
          put("model", MODEL)
          put("max_tokens", 512)
          put("tools", JSONArray().put(tool))
          put("tool_choice", JSONObject().apply {
            put("type", "tool")
            put("name", TOOL_NAME)
          })
          put("messages", JSONArray().put(message))
        }
        .toString()
  }

  private fun post(body: String): String? {
    val req =
        Request.Builder()
            .url(URL)
            .post(body.toRequestBody(JSON))
            .header("x-api-key", apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header("content-type", "application/json")
            .build()
    return try {
      client.newCall(req).execute().use { resp ->
        val text = resp.body?.string()
        if (!resp.isSuccessful) {
          Log.e(TAG, "Anthropic ${resp.code}: ${text?.take(400)}")
          null
        } else text
      }
    } catch (e: IOException) {
      Log.e(TAG, "Request failed", e)
      null
    }
  }

  private fun parseDetection(responseJson: String): FoodDetection? {
    return try {
      val content = JSONObject(responseJson).optJSONArray("content") ?: return null
      var input: JSONObject? = null
      for (i in 0 until content.length()) {
        val block = content.getJSONObject(i)
        if (block.optString("type") == "tool_use" && block.optString("name") == TOOL_NAME) {
          input = block.optJSONObject("input")
          break
        }
      }
      if (input == null) return null
      val rawLabel = input.opt("label")?.takeIf { it != JSONObject.NULL }?.toString()?.trim()
      val label = rawLabel?.lowercase()?.takeIf { it.isNotEmpty() }
      val name = input.optString("name").trim().ifEmpty { return null }
      val known = label?.let { knownFoods[it] }
      FoodDetection(
          label = label,
          name = known?.displayName ?: name,
          calories = known?.calories ?: input.optInt("calories"),
          proteinG = known?.proteinG ?: input.optDouble("protein_g", 0.0),
          carbsG = known?.carbsG ?: input.optDouble("carbs_g", 0.0),
          fatG = known?.fatG ?: input.optDouble("fat_g", 0.0),
          isJunk = input.optBoolean("is_junk", false),
          wasEstimated = known == null,
      )
    } catch (e: Exception) {
      Log.e(TAG, "Parse failed", e)
      null
    }
  }
}

package com.vitalis.assistant

import android.graphics.Bitmap
import android.util.Log
import com.vitalis.foodlog.FoodDetection
import com.vitalis.foodlog.FoodInfo
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

private const val TAG = "Vitalis:AnthropicFoodDetector"
private const val MESSAGES_URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"
private const val MODEL = "claude-haiku-4-5-20251001"
private const val TOOL_NAME = "log_foods"

private const val GATE_PROMPT =
    "You are a fast visual classifier. This image is a first-person POV from smart glasses — what " +
        "the wearer is actually looking at. Decide whether the wearer is about to eat or drink " +
        "something RIGHT NOW. Answer YES only if a food or drink item is CLOSE to the wearer: " +
        "directly in their hands, at their place setting, on the near edge of the table in front " +
        "of them (within arm's reach), or being raised toward their mouth. Answer NO if the food " +
        "is across the table, on someone else's plate, on a distant counter, behind glass, on a " +
        "menu/poster/screen, or otherwise out of reach. Also NO for empty scenes (walls, " +
        "furniture, scenery, people without visible close food). Respond with exactly one word: " +
        "YES or NO. No punctuation, no explanation."

private val JSON = "application/json".toMediaType()

class AnthropicFoodDetector(
    private val apiKey: String,
    private val knownFoods: Map<String, FoodInfo>,
) {

  private val client =
      OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(45, TimeUnit.SECONDS)
          .build()

  /**
   * Sends the frame to Haiku with the [TOOL_NAME] tool definition. Returns the parsed list of
   * detections (possibly empty). On network or parse failure returns an empty list so the calling
   * loop can keep ticking.
   */
  suspend fun detect(bitmap: Bitmap, context: PromptContext): List<FoodDetection> =
      withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
          Log.w(TAG, "Missing ANTHROPIC_API_KEY — skipping detection")
          return@withContext emptyList()
        }
        // Cheap pre-gate: if the scene clearly contains no food, skip the expensive tool call.
        if (!isFoodScene(bitmap)) return@withContext emptyList()

        val imageB64 = BitmapEncoding.toBase64Jpeg(bitmap, maxDim = 1024, quality = 85)
        val body = buildBody(imageB64, context)
        val responseText = post(body) ?: return@withContext emptyList()
        parseDetections(responseText)
      }

  /** Yes/no gate run before the heavy tool-use detector. */
  suspend fun isFoodScene(bitmap: Bitmap): Boolean =
      withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        val imageB64 = BitmapEncoding.toBase64Jpeg(bitmap, maxDim = 768, quality = 70)
        val body = buildClassifierBody(imageB64, GATE_PROMPT)
        val responseText = post(body) ?: return@withContext false
        val text = extractFirstText(responseText)?.trim()?.uppercase().orEmpty()
        val verdict = text.startsWith("YES")
        Log.d(TAG, "Food-scene gate -> '$text' (verdict=$verdict)")
        verdict
      }

  private fun buildBody(imageBase64: String, context: PromptContext): String {
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
      put("text", buildPrompt(context))
    }
    val message =
        JSONObject().apply {
          put("role", "user")
          put("content", JSONArray().put(imagePart).put(textPart))
        }
    return JSONObject()
        .apply {
          put("model", MODEL)
          put("max_tokens", 1024)
          put("tools", JSONArray().put(toolDefinition()))
          put(
              "tool_choice",
              JSONObject().apply {
                put("type", "tool")
                put("name", TOOL_NAME)
              },
          )
          put("messages", JSONArray().put(message))
        }
        .toString()
  }

  private fun toolDefinition(): JSONObject {
    val foodItem =
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
                  .put("label")
                  .put("name")
                  .put("calories")
                  .put("protein_g")
                  .put("carbs_g")
                  .put("fat_g")
                  .put("is_junk"),
          )
        }
    val foodsArray =
        JSONObject().apply {
          put("type", "array")
          put("items", foodItem)
        }
    val inputSchema =
        JSONObject().apply {
          put("type", "object")
          put("properties", JSONObject().apply { put("foods", foodsArray) })
          put("required", JSONArray().put("foods"))
        }
    return JSONObject().apply {
      put("name", TOOL_NAME)
      put(
          "description",
          "Report every distinct food or drink the user could eat or drink in the frame.",
      )
      put("input_schema", inputSchema)
    }
  }

  private fun buildPrompt(context: PromptContext): String {
    val knownLabels =
        knownFoods.keys.sorted().joinToString(", ").ifBlank { "(none — estimate everything)" }
    val recent =
        if (context.recentLabels.isEmpty()) "(nothing logged yet this session)"
        else context.recentLabels.joinToString(", ")

    return """
        You are a nutrition vision assistant analyzing a live first-person camera frame from the
        wearer's smart glasses. Identify ONLY food or drink items that are CLOSE to the wearer —
        in their hands, at their place setting, on the near edge of the table within arm's reach,
        or being raised toward their mouth. Ignore food across the table, on someone else's plate,
        on a distant counter, in a display case, on a menu/poster/screen, or otherwise out of reach.
        Report every qualifying item by calling $TOOL_NAME. For each item:
          - label: if it clearly matches one of the KNOWN LABELS, copy that label EXACTLY;
            otherwise null.
          - name: a short, canonical, lowercase noun for the item with no modifiers — e.g. "coffee"
            not "cup of hot coffee", "burger" not "cheeseburger on a plate". This name is used for
            deduplication, so be consistent across calls when shown the same item.
          - calories, protein_g, carbs_g, fat_g: best estimate for ONE typical serving.
          - is_junk: true if it is junk food (deep-fried, added sugar, refined carbs, alcohol) OR
            if eating it now would push the user FURTHER from today's macro targets given their
            current totals (e.g. carbs already at 90% of target → another carb-heavy item is junk).

        If there is no food or drink in the frame, return an empty foods array.

        USER CONTEXT (incorporate when choosing is_junk):
        ${context.toSystemBlurb()}

        KNOWN LABELS: $knownLabels

        ALREADY LOGGED THIS SESSION (these are normalized keys — do NOT re-report an item that is
        conceptually the same, even if you would phrase it slightly differently): $recent
        """.trimIndent()
  }

  private fun buildClassifierBody(imageBase64: String, textPrompt: String): String {
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
    val message =
        JSONObject().apply {
          put("role", "user")
          put("content", JSONArray().put(imagePart).put(textPart))
        }
    return JSONObject()
        .apply {
          put("model", MODEL)
          put("max_tokens", 5)
          put("messages", JSONArray().put(message))
        }
        .toString()
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
        Log.e(TAG, "Failed to parse classifier response", e)
        null
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

  private fun parseDetections(responseJson: String): List<FoodDetection> {
    return try {
      val contentArr = JSONObject(responseJson).optJSONArray("content") ?: return emptyList()
      var toolInput: JSONObject? = null
      for (i in 0 until contentArr.length()) {
        val block = contentArr.getJSONObject(i)
        if (block.optString("type") == "tool_use" && block.optString("name") == TOOL_NAME) {
          toolInput = block.optJSONObject("input")
          break
        }
      }
      val foods = toolInput?.optJSONArray("foods") ?: return emptyList()
      buildList {
        for (i in 0 until foods.length()) {
          val f = foods.getJSONObject(i)
          val rawLabel = f.opt("label")?.takeIf { it != JSONObject.NULL }?.toString()?.trim()
          val label = rawLabel?.lowercase()?.takeIf { it.isNotEmpty() }
          val name = f.optString("name").trim()
          if (name.isEmpty()) continue
          val known = label?.let { knownFoods[it] }
          add(
              FoodDetection(
                  label = label,
                  name = known?.displayName ?: name,
                  calories = known?.calories ?: f.optInt("calories"),
                  proteinG = known?.proteinG ?: f.optDouble("protein_g", 0.0),
                  carbsG = known?.carbsG ?: f.optDouble("carbs_g", 0.0),
                  fatG = known?.fatG ?: f.optDouble("fat_g", 0.0),
                  isJunk = f.optBoolean("is_junk", false),
                  wasEstimated = known == null,
              )
          )
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to parse food-detection tool_use payload", e)
      emptyList()
    }
  }
}

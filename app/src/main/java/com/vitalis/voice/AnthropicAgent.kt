package com.vitalis.voice

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

private const val TAG = "Vitalis:AnthropicAgent"
private const val MESSAGES_URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION = "2023-06-01"
private const val MODEL = "claude-haiku-4-5-20251001"
private const val MAX_TOKENS = 512

private val JSON = "application/json".toMediaType()

const val TOOL_START_MENU_SCAN = "start_menu_scan"
const val TOOL_FIND_RESTAURANT = "find_restaurant"
const val TOOL_SUMMARIZE_FOOD_LOG = "summarize_food_log"

sealed interface AgentDecision {
  data class ToolCall(
      val id: String,
      val name: String,
      val input: JSONObject,
      /** Raw assistant content (text + tool_use) so we can roundtrip a tool_result. */
      val rawAssistantContent: JSONArray,
  ) : AgentDecision

  data class TextResponse(val text: String) : AgentDecision

  data object Empty : AgentDecision
}

/**
 * Backwards-compatible alias — the voice agent takes a [PromptContext] (profile + macro balance +
 * recent labels) plus the spoken transcript.
 */
typealias AgentContext = PromptContext

class AnthropicAgent(private val apiKey: String) {

  private val client =
      OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .build()

  /** First-pass call: send the transcript + context, get a tool call or a text response. */
  suspend fun decide(transcript: String, context: AgentContext): AgentDecision =
      withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
          Log.w(TAG, "Missing ANTHROPIC_API_KEY")
          return@withContext AgentDecision.Empty
        }
        val body = buildInitialBody(transcript, context)
        val resp = post(body) ?: return@withContext AgentDecision.Empty
        parseDecision(resp)
      }

  /**
   * Follow-up after we executed a tool that returns data back to the model (currently only
   * [TOOL_SUMMARIZE_FOOD_LOG]). Returns the model's natural-language reply.
   */
  suspend fun continueAfterTool(
      transcript: String,
      context: AgentContext,
      previousAssistantContent: JSONArray,
      toolUseId: String,
      toolResultText: String,
  ): String? =
      withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val body =
            buildContinuationBody(
                transcript,
                context,
                previousAssistantContent,
                toolUseId,
                toolResultText,
            )
        val resp = post(body) ?: return@withContext null
        extractText(resp)
      }

  // ---------------------- request building ----------------------

  private fun buildInitialBody(transcript: String, context: AgentContext): String {
    val userMessage =
        JSONObject().apply {
          put("role", "user")
          put("content", JSONArray().put(textPart(transcript)))
        }
    return JSONObject()
        .apply {
          put("model", MODEL)
          put("max_tokens", MAX_TOKENS)
          put("system", systemPrompt(context))
          put("tools", toolDefs())
          put(
              "tool_choice",
              JSONObject().apply { put("type", "auto") },
          )
          put("messages", JSONArray().put(userMessage))
        }
        .toString()
  }

  private fun buildContinuationBody(
      transcript: String,
      context: AgentContext,
      previousAssistantContent: JSONArray,
      toolUseId: String,
      toolResultText: String,
  ): String {
    val userMessage =
        JSONObject().apply {
          put("role", "user")
          put("content", JSONArray().put(textPart(transcript)))
        }
    val assistantMessage =
        JSONObject().apply {
          put("role", "assistant")
          put("content", previousAssistantContent)
        }
    val toolResult =
        JSONObject().apply {
          put("type", "tool_result")
          put("tool_use_id", toolUseId)
          put("content", toolResultText)
        }
    val toolResultMessage =
        JSONObject().apply {
          put("role", "user")
          put("content", JSONArray().put(toolResult))
        }
    return JSONObject()
        .apply {
          put("model", MODEL)
          put("max_tokens", MAX_TOKENS)
          put("system", systemPrompt(context))
          put("tools", toolDefs())
          put(
              "messages",
              JSONArray().put(userMessage).put(assistantMessage).put(toolResultMessage),
          )
        }
        .toString()
  }

  private fun systemPrompt(context: AgentContext): String {
    return """
        You are Vitalis, a heads-up nutrition assistant running on Ray-Ban Meta glasses. The user
        just spoke to you; their words are in the user message. Decide what to do.

        Rules:
        - Prefer calling a tool when the user's intent matches one. Otherwise reply in plain text.
        - When the user expresses a preference inside a tool-worthy request ("scan the menu, I like
          steak" / "find pizza, low carb please"), preserve that preference verbatim in the tool's
          query argument so the downstream model can use it.
        - When recommending or answering, integrate ALL of: profile, DNA notes, today's macros vs
          targets, current imbalances. Don't say things at odds with their stated goals.
        - Text replies must be ONE sentence, max ~25 words, spoken aloud via TTS, no markdown.
        - Tools available:
          • $TOOL_START_MENU_SCAN — they're looking at a menu and want a recommendation. Include
            their stated preference (if any) in the `preference` arg so the menu picker uses it.
          • $TOOL_FIND_RESTAURANT(query) — they want to find a nearby restaurant for a craving.
          • $TOOL_SUMMARIZE_FOOD_LOG(period_hours) — they're asking about what they've eaten.

        USER CONTEXT:
        ${context.toSystemBlurb()}
        """.trimIndent()
  }

  private fun toolDefs(): JSONArray {
    val menuTool =
        JSONObject().apply {
          put("name", TOOL_START_MENU_SCAN)
          put(
              "description",
              "Activate the menu scanner. Use when the user says things like 'scan this menu', " +
                  "'what should I order', 'help me pick'. Pass any preference the user mentioned " +
                  "(e.g. 'I like steak', 'low carb') as the `preference` field.",
          )
          put(
              "input_schema",
              JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject().apply {
                      put(
                          "preference",
                          JSONObject().apply {
                            put("type", JSONArray().put("string").put("null"))
                            put("description", "User's stated preference, in their own words. Null if none.")
                          },
                      )
                    },
                )
                put("required", JSONArray())
              },
          )
        }
    val findTool =
        JSONObject().apply {
          put("name", TOOL_FIND_RESTAURANT)
          put(
              "description",
              "Find an open restaurant nearby matching a craving. Use for 'find ramen', " +
                  "'where can I get sushi', 'I want vegan tacos'.",
          )
          put(
              "input_schema",
              JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject().apply {
                      put(
                          "query",
                          JSONObject().apply {
                            put("type", "string")
                            put("description", "What the user is craving, in their own words.")
                          },
                      )
                    },
                )
                put("required", JSONArray().put("query"))
              },
          )
        }
    val summaryTool =
        JSONObject().apply {
          put("name", TOOL_SUMMARIZE_FOOD_LOG)
          put(
              "description",
              "Read recent food log entries and reply with totals. Use for 'what have I eaten', " +
                  "'how many calories so far', 'how am I doing'.",
          )
          put(
              "input_schema",
              JSONObject().apply {
                put("type", "object")
                put(
                    "properties",
                    JSONObject().apply {
                      put(
                          "period_hours",
                          JSONObject().apply {
                            put("type", "number")
                            put("description", "Lookback window in hours. Default 24.")
                          },
                      )
                    },
                )
                put("required", JSONArray())
              },
          )
        }
    return JSONArray().put(menuTool).put(findTool).put(summaryTool)
  }

  private fun textPart(text: String): JSONObject =
      JSONObject().apply {
        put("type", "text")
        put("text", text)
      }

  // ---------------------- HTTP ----------------------

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
      Log.e(TAG, "Request failed", e)
      null
    }
  }

  // ---------------------- response parsing ----------------------

  private fun parseDecision(responseJson: String): AgentDecision {
    return try {
      val root = JSONObject(responseJson)
      val content = root.optJSONArray("content") ?: return AgentDecision.Empty
      var toolCall: AgentDecision.ToolCall? = null
      var collectedText = StringBuilder()
      for (i in 0 until content.length()) {
        val block = content.getJSONObject(i)
        when (block.optString("type")) {
          "tool_use" -> {
            toolCall =
                AgentDecision.ToolCall(
                    id = block.optString("id"),
                    name = block.optString("name"),
                    input = block.optJSONObject("input") ?: JSONObject(),
                    rawAssistantContent = content,
                )
          }
          "text" -> collectedText.append(block.optString("text"))
        }
      }
      toolCall?.let {
        Log.d(TAG, "Tool call -> ${it.name}(${it.input})")
        return it
      }
      val text = collectedText.toString().trim()
      if (text.isEmpty()) AgentDecision.Empty else AgentDecision.TextResponse(text)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to parse decision", e)
      AgentDecision.Empty
    }
  }

  private fun extractText(responseJson: String): String? =
      try {
        val arr = JSONObject(responseJson).optJSONArray("content") ?: return null
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
          val block = arr.getJSONObject(i)
          if (block.optString("type") == "text") sb.append(block.optString("text"))
        }
        sb.toString().trim().takeIf { it.isNotEmpty() }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to parse text response", e)
        null
      }
}

package com.vitalis.voice

import android.content.Context
import android.util.Log
import com.vitalis.assistant.AudioPlayback
import com.vitalis.assistant.ElevenLabsClient
import com.vitalis.foodlog.FoodLogRepository
import com.vitalis.foodlog.FoodLogSummary
import com.vitalis.profile.PromptContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "Vitalis:VoiceController"

/**
 * Drives one voice turn: STT → agent → execute tool / speak response. State changes are pushed to
 * the host via [publishState].
 *
 * The host (AssistantViewModel) supplies a [PromptContext] each turn so the agent sees fresh
 * profile + macro-balance information without us re-querying repositories here.
 */
class VoiceController(
    private val context: Context,
    private val audio: AudioPlayback,
    private val foodLog: FoodLogRepository,
    private val publishState: (VoiceState) -> Unit,
    private val onStartMenuScan: (preference: String?) -> Unit,
    private val onFindRestaurant: (query: String) -> Unit,
) {

  private val recognizer = SpeechRecognizerWrapper(context)
  private var currentJob: Job? = null

  fun cancel() {
    currentJob?.cancel()
    currentJob = null
    publishState(VoiceState(VoicePhase.IDLE))
  }

  fun startTurn(
      scope: CoroutineScope,
      promptContext: PromptContext,
      anthropicKey: String,
      ttsKey: String,
      ttsVoiceId: String,
  ) {
    currentJob?.cancel()
    currentJob =
        scope.launch {
          if (anthropicKey.isBlank()) {
            publishState(
                VoiceState(
                    phase = VoicePhase.ERROR,
                    errorMessage =
                        "ANTHROPIC_API_KEY missing from local.properties — voice assistant disabled.",
                )
            )
            return@launch
          }

          publishState(VoiceState(phase = VoicePhase.LISTENING))
          val partialJob =
              launch {
                recognizer.partialTranscript.collect { partial ->
                  publishState(VoiceState(phase = VoicePhase.LISTENING, transcript = partial))
                }
              }

          val transcript =
              try {
                recognizer.recognize()
              } catch (e: SpeechRecognitionUnavailable) {
                publishState(
                    VoiceState(
                        phase = VoicePhase.ERROR,
                        errorMessage = "Speech recognition isn't available on this device.",
                    )
                )
                partialJob.cancel()
                return@launch
              } catch (e: Exception) {
                Log.e(TAG, "STT failed", e)
                publishState(
                    VoiceState(
                        phase = VoicePhase.ERROR,
                        errorMessage = "Voice recognition failed. Try again.",
                    )
                )
                partialJob.cancel()
                return@launch
              }
          partialJob.cancel()

          if (transcript.isNullOrBlank()) {
            publishState(
                VoiceState(
                    phase = VoicePhase.ERROR,
                    errorMessage = "Didn't catch that. Tap the mic and try again.",
                )
            )
            return@launch
          }

          publishState(VoiceState(phase = VoicePhase.THINKING, transcript = transcript))

          val agent = AnthropicAgent(anthropicKey)
          val decision =
              runCatching { agent.decide(transcript, promptContext) }
                  .onFailure { Log.e(TAG, "Agent call failed", it) }
                  .getOrDefault(AgentDecision.Empty)

          when (decision) {
            is AgentDecision.ToolCall ->
                handleToolCall(
                    decision = decision,
                    transcript = transcript,
                    agent = agent,
                    context = promptContext,
                    ttsKey = ttsKey,
                    ttsVoiceId = ttsVoiceId,
                )
            is AgentDecision.TextResponse ->
                speakAndIdle(decision.text, transcript, ttsKey, ttsVoiceId)
            AgentDecision.Empty ->
                publishState(
                    VoiceState(
                        phase = VoicePhase.ERROR,
                        transcript = transcript,
                        errorMessage = "No response. Try again.",
                    )
                )
          }
        }
  }

  private suspend fun handleToolCall(
      decision: AgentDecision.ToolCall,
      transcript: String,
      agent: AnthropicAgent,
      context: PromptContext,
      ttsKey: String,
      ttsVoiceId: String,
  ) {
    val now = System.currentTimeMillis()
    when (decision.name) {
      TOOL_START_MENU_SCAN -> {
        val preference =
            decision.input.opt("preference")?.takeIf { it != org.json.JSONObject.NULL }
                ?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        Log.d(TAG, "Tool: start_menu_scan preference='$preference'")
        onStartMenuScan(preference)
        publishState(VoiceState(phase = VoicePhase.IDLE))
      }
      TOOL_FIND_RESTAURANT -> {
        val query = decision.input.optString("query").trim()
        Log.d(TAG, "Tool: find_restaurant query='$query'")
        if (query.isEmpty()) {
          publishState(
              VoiceState(
                  phase = VoicePhase.ERROR,
                  transcript = transcript,
                  errorMessage = "I didn't catch what you were looking for. Try again.",
              )
          )
        } else {
          onFindRestaurant(query)
          publishState(VoiceState(phase = VoicePhase.IDLE))
        }
      }
      TOOL_SUMMARIZE_FOOD_LOG -> {
        val hours = decision.input.optDouble("period_hours", 24.0).toInt().coerceIn(1, 168)
        Log.d(TAG, "Tool: summarize_food_log periodHours=$hours")
        val summary =
            runCatching { foodLog.summarize(now - hours * 3_600_000L) }
                .getOrDefault(FoodLogSummary(0, 0, 0.0, 0.0, 0.0, 0, emptyList()))
        val resultText = formatSummaryForAgent(summary, hours)
        val followUp =
            runCatching {
                  agent.continueAfterTool(
                      transcript = transcript,
                      context = context,
                      previousAssistantContent = decision.rawAssistantContent,
                      toolUseId = decision.id,
                      toolResultText = resultText,
                  )
                }
                .onFailure { Log.e(TAG, "continueAfterTool failed", it) }
                .getOrNull()
        if (followUp.isNullOrBlank()) {
          publishState(
              VoiceState(
                  phase = VoicePhase.ERROR,
                  transcript = transcript,
                  errorMessage = "Couldn't generate a summary.",
              )
          )
        } else {
          speakAndIdle(followUp, transcript, ttsKey, ttsVoiceId)
        }
      }
      else -> {
        Log.w(TAG, "Unknown tool: ${decision.name}")
        publishState(
            VoiceState(
                phase = VoicePhase.ERROR,
                transcript = transcript,
                errorMessage = "Unknown action: ${decision.name}",
            )
        )
      }
    }
  }

  private suspend fun speakAndIdle(
      text: String,
      transcript: String,
      ttsKey: String,
      ttsVoiceId: String,
  ) {
    publishState(
        VoiceState(phase = VoicePhase.SPEAKING, transcript = transcript, response = text)
    )
    if (ttsKey.isNotBlank() && ttsVoiceId.isNotBlank()) {
      val tts = ElevenLabsClient(ttsKey, ttsVoiceId)
      val bytes = runCatching { tts.synthesize(text) }.getOrNull()
      if (bytes != null) {
        runCatching { audio.playTtsBytes(bytes) }
            .onFailure { Log.e(TAG, "TTS playback failed", it) }
      }
    } else {
      Log.d(TAG, "Skipping TTS — ElevenLabs key/voice missing")
    }
    publishState(
        VoiceState(phase = VoicePhase.IDLE, transcript = transcript, response = text)
    )
  }

  private fun formatSummaryForAgent(s: FoodLogSummary, hours: Int): String =
      if (s.entryCount == 0) {
        "No food logged in the past $hours hours."
      } else {
        val items = if (s.topItems.isEmpty()) "(none)" else s.topItems.joinToString(", ")
        "Past $hours hours: ${s.entryCount} items, ${s.totalCalories} kcal total " +
            "(${"%.0f".format(s.totalProteinG)}g protein, ${"%.0f".format(s.totalCarbsG)}g carbs, " +
            "${"%.0f".format(s.totalFatG)}g fat). ${s.junkCount} flagged as junk. " +
            "Recent items: $items."
      }
}

package com.vitalis.voice

enum class VoicePhase {
  IDLE,
  LISTENING,
  THINKING,
  SPEAKING,
  ERROR,
}

data class VoiceState(
    val phase: VoicePhase = VoicePhase.IDLE,
    /** Live partial transcript while LISTENING, finalized transcript afterward. */
    val transcript: String = "",
    /** Plain-text response from the agent that we're speaking / showing. */
    val response: String? = null,
    val errorMessage: String? = null,
)

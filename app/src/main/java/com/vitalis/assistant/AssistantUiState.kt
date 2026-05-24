package com.vitalis.assistant

import android.graphics.Bitmap
import android.graphics.Rect

enum class AssistantPhase {
  /** Default: stream + passive food sampling running. */
  LOGGING,
  /** Menu classifier loop active. Food sampler paused. */
  MENU_SCANNING,
  /** Menu detected; OCR + Sonnet generating recommendations on the captured still. */
  MENU_ANALYZING,
  /** Pulsating dot overlay shown on the captured menu still. */
  MENU_RESULTS,
  ERROR,
}

data class MenuRecommendation(
    val id: String,
    val itemName: String,
    val reason: String,
    /** Bounding box in [AssistantUiState.capturedMenu] pixel coordinates. */
    val box: Rect,
    val pexelsImageUrl: String?,
)

data class AssistantUiState(
    val phase: AssistantPhase = AssistantPhase.LOGGING,
    val capturedMenu: Bitmap? = null,
    val recommendations: List<MenuRecommendation> = emptyList(),
    val selectedRecommendationId: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

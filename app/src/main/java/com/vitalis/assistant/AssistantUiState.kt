package com.vitalis.assistant

import android.graphics.Bitmap
import android.graphics.Rect
import com.vitalis.placesearch.Restaurant
import com.vitalis.voice.VoiceState

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

enum class RestaurantSearchPhase {
  IDLE,
  /** Waiting for the user to grant location permission. */
  NEEDS_LOCATION_PERMISSION,
  /** Fetching device location or hitting Places API. */
  LOADING,
  RESULTS,
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

data class RestaurantSearchState(
    val visible: Boolean = false,
    val phase: RestaurantSearchPhase = RestaurantSearchPhase.IDLE,
    val query: String = "",
    val results: List<Restaurant> = emptyList(),
    val errorMessage: String? = null,
)

data class AssistantUiState(
    val phase: AssistantPhase = AssistantPhase.LOGGING,
    val capturedMenu: Bitmap? = null,
    val recommendations: List<MenuRecommendation> = emptyList(),
    val selectedRecommendationId: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val restaurantSearch: RestaurantSearchState = RestaurantSearchState(),
    val voice: VoiceState = VoiceState(),
)

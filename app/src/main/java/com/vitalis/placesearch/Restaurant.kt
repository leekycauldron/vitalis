package com.vitalis.placesearch

enum class TravelMode {
  WALK,
  DRIVE,
}

data class TravelEstimate(
    val mode: TravelMode,
    val distanceMeters: Int,
    val durationMinutes: Int,
) {
  fun formatted(): String {
    val verb = if (mode == TravelMode.WALK) "walk" else "drive"
    val dist =
        if (distanceMeters < 1000) "${distanceMeters} m"
        else "%.1f km".format(distanceMeters / 1000.0)
    return "$durationMinutes min $verb · $dist"
  }
}

data class Restaurant(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val openNow: Boolean?,
    val rating: Double?,
    val ratingCount: Int?,
    /** Google's PRICE_LEVEL_* enum, mapped to $/$$/$$$. Null if unknown. */
    val priceLevel: String?,
    val travel: TravelEstimate,
)

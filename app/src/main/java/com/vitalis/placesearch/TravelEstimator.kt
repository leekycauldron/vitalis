package com.vitalis.placesearch

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cheap travel-time heuristic: haversine for distance, then walk if it's a reasonable foot trip,
 * otherwise drive. Avoids a second Routes API billing per result. Replace with Routes API later
 * if we want traffic-aware times.
 */
object TravelEstimator {

  private const val EARTH_RADIUS_METERS = 6_371_000.0
  private const val WALK_THRESHOLD_METERS = 1_500
  private const val WALK_SPEED_M_PER_MIN = 83.0 // ~5 km/h
  private const val DRIVE_SPEED_M_PER_MIN = 500.0 // ~30 km/h urban

  fun estimate(
      fromLat: Double,
      fromLng: Double,
      toLat: Double,
      toLng: Double,
  ): TravelEstimate {
    val distance = haversineMeters(fromLat, fromLng, toLat, toLng).roundToInt()
    val mode = if (distance <= WALK_THRESHOLD_METERS) TravelMode.WALK else TravelMode.DRIVE
    val speed = if (mode == TravelMode.WALK) WALK_SPEED_M_PER_MIN else DRIVE_SPEED_M_PER_MIN
    val duration = (distance / speed).roundToInt().coerceAtLeast(1)
    return TravelEstimate(mode, distance, duration)
  }

  private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val φ1 = Math.toRadians(lat1)
    val φ2 = Math.toRadians(lat2)
    val Δφ = Math.toRadians(lat2 - lat1)
    val Δλ = Math.toRadians(lng2 - lng1)
    val a = sin(Δφ / 2) * sin(Δφ / 2) + cos(φ1) * cos(φ2) * sin(Δλ / 2) * sin(Δλ / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
  }
}

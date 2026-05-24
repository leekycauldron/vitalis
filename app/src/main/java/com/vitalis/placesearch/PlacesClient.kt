package com.vitalis.placesearch

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

private const val TAG = "Vitalis:PlacesClient"
private const val URL = "https://places.googleapis.com/v1/places:searchText"
private const val SEARCH_RADIUS_METERS = 10_000.0
private const val MAX_RESULTS = 15
private const val FIELD_MASK =
    "places.id,places.displayName,places.formattedAddress,places.location," +
        "places.currentOpeningHours.openNow,places.regularOpeningHours.openNow," +
        "places.rating,places.userRatingCount,places.priceLevel"

private val JSON = "application/json".toMediaType()

/** Google Places API (New) — Text Search. */
class PlacesClient(private val apiKey: String) {

  private val client =
      OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(20, TimeUnit.SECONDS)
          .build()

  suspend fun searchRestaurants(
      query: String,
      userLat: Double,
      userLng: Double,
  ): List<Restaurant> =
      withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
          Log.w(TAG, "Missing GOOGLE_PLACES_API_KEY — skipping search")
          return@withContext emptyList()
        }
        val body = buildBody(query, userLat, userLng)
        val req =
            Request.Builder()
                .url(URL)
                .post(body.toRequestBody(JSON))
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .header("Content-Type", "application/json")
                .build()
        val raw =
            try {
              client.newCall(req).execute().use { resp ->
                val text = resp.body?.string()
                if (!resp.isSuccessful) {
                  Log.e(TAG, "Places ${resp.code}: ${text?.take(500)}")
                  return@withContext emptyList()
                }
                text
              }
            } catch (e: IOException) {
              Log.e(TAG, "Places request failed", e)
              return@withContext emptyList()
            }
        parseResults(raw.orEmpty(), userLat, userLng)
      }

  private fun buildBody(query: String, lat: Double, lng: Double): String {
    val center = JSONObject().apply {
      put("latitude", lat)
      put("longitude", lng)
    }
    val circle = JSONObject().apply {
      put("center", center)
      put("radius", SEARCH_RADIUS_METERS)
    }
    val locationBias = JSONObject().apply { put("circle", circle) }
    return JSONObject()
        .apply {
          put("textQuery", query)
          put("locationBias", locationBias)
          put("includedType", "restaurant")
          put("openNow", true)
          put("maxResultCount", MAX_RESULTS)
        }
        .toString()
  }

  private fun parseResults(
      responseJson: String,
      userLat: Double,
      userLng: Double,
  ): List<Restaurant> {
    return try {
      val places = JSONObject(responseJson).optJSONArray("places") ?: JSONArray()
      buildList {
        for (i in 0 until places.length()) {
          val p = places.getJSONObject(i)
          val location = p.optJSONObject("location") ?: continue
          val lat = location.optDouble("latitude", Double.NaN)
          val lng = location.optDouble("longitude", Double.NaN)
          if (lat.isNaN() || lng.isNaN()) continue
          val name = p.optJSONObject("displayName")?.optString("text").orEmpty()
          if (name.isBlank()) continue
          val address = p.optString("formattedAddress").orEmpty()
          val open =
              p.optJSONObject("currentOpeningHours")?.optBooleanOrNull("openNow")
                  ?: p.optJSONObject("regularOpeningHours")?.optBooleanOrNull("openNow")
          add(
              Restaurant(
                  id = p.optString("id"),
                  name = name,
                  address = address,
                  latitude = lat,
                  longitude = lng,
                  openNow = open,
                  rating = p.optDoubleOrNull("rating"),
                  ratingCount = p.optIntOrNull("userRatingCount"),
                  priceLevel = p.optStringOrNull("priceLevel"),
                  travel = TravelEstimator.estimate(userLat, userLng, lat, lng),
              )
          )
        }
      }.sortedBy { it.travel.durationMinutes }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to parse Places response", e)
      emptyList()
    }
  }

  private fun JSONObject.optBooleanOrNull(key: String): Boolean? =
      if (has(key) && !isNull(key)) optBoolean(key) else null

  private fun JSONObject.optDoubleOrNull(key: String): Double? =
      if (has(key) && !isNull(key)) optDouble(key) else null

  private fun JSONObject.optIntOrNull(key: String): Int? =
      if (has(key) && !isNull(key)) optInt(key) else null

  private fun JSONObject.optStringOrNull(key: String): String? =
      if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotEmpty() } else null
}

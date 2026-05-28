package com.vitalis.foodlog

import android.util.Log
import com.vitalis.BuildConfig
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val TAG = "Vitalis:MealApi"
private val JSON = "application/json".toMediaType()

class MealApiClient(
    private val url: String = BuildConfig.MEAL_API_URL,
    private val secret: String = BuildConfig.MEAL_API_SECRET,
) {

  private val client =
      OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .build()

  suspend fun logMeal(
      name: String,
      calories: Int,
      protein: Double,
      carbs: Double,
      fat: Double,
      loggedAtMs: Long,
  ): Boolean =
      withContext(Dispatchers.IO) {
        if (url.isBlank() || secret.isBlank()) {
          Log.d(TAG, "Skipping remote log — MEAL_API_URL or MEAL_API_SECRET not configured")
          return@withContext false
        }
        val payload =
            JSONObject()
                .apply {
                  put("name", name)
                  put("calories", calories)
                  put("protein", protein)
                  put("carbs", carbs)
                  put("fat", fat)
                  put("logged_at", formatIso8601(loggedAtMs))
                }
                .toString()
        val req =
            Request.Builder()
                .url(url)
                .post(payload.toRequestBody(JSON))
                .header("Content-Type", "application/json")
                .header("X-Appointment-Webhook-Secret", secret)
                .build()
        try {
          client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
              Log.w(TAG, "Remote log failed ${resp.code}: ${resp.body?.string()?.take(300)}")
              false
            } else {
              Log.d(TAG, "Remote log OK for '$name'")
              true
            }
          }
        } catch (e: IOException) {
          Log.w(TAG, "Remote log request failed for '$name'", e)
          false
        }
      }

  private fun formatIso8601(epochMs: Long): String =
      Instant.ofEpochMilli(epochMs)
          .atZone(ZoneId.systemDefault())
          .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

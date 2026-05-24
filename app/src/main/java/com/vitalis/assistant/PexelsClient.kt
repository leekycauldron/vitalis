package com.vitalis.assistant

import android.util.Log
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val TAG = "Vitalis:PexelsClient"
private const val SEARCH_URL = "https://api.pexels.com/v1/search"

class PexelsClient(private val apiKey: String) {

  private val client =
      OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(15, TimeUnit.SECONDS)
          .build()

  suspend fun firstImageUrl(query: String): String? =
      withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val q = URLEncoder.encode(query, "UTF-8")
        val req =
            Request.Builder()
                .url("$SEARCH_URL?query=$q&per_page=1&orientation=square")
                .header("Authorization", apiKey)
                .get()
                .build()
        try {
          client.newCall(req).execute().use { resp ->
            val body = resp.body?.string()
            if (!resp.isSuccessful || body == null) {
              Log.w(TAG, "Pexels ${resp.code}: ${body?.take(300)}")
              return@withContext null
            }
            val photos = JSONObject(body).optJSONArray("photos") ?: return@withContext null
            if (photos.length() == 0) return@withContext null
            val src = photos.getJSONObject(0).optJSONObject("src") ?: return@withContext null
            // medium is a reasonable size for a card; fall back through the available sizes
            src.optString("medium").takeIf { it.isNotEmpty() }
                ?: src.optString("small").takeIf { it.isNotEmpty() }
                ?: src.optString("original").takeIf { it.isNotEmpty() }
          }
        } catch (e: IOException) {
          Log.e(TAG, "Pexels request failed", e)
          null
        }
      }
}

package com.vitalis.assistant

import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val TAG = "Vitalis:ElevenLabsClient"
private const val BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech/"
private const val MODEL_ID = "eleven_turbo_v2_5"
private val JSON = "application/json".toMediaType()

class ElevenLabsClient(private val apiKey: String, private val voiceId: String) {

  private val client =
      OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .build()

  /** Returns MP3 bytes for the given text. null if the API call fails or keys are missing. */
  suspend fun synthesize(text: String): ByteArray? =
      withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || voiceId.isBlank()) {
          Log.w(TAG, "Missing ElevenLabs key or voice id — skipping TTS")
          return@withContext null
        }
        val body =
            JSONObject()
                .apply {
                  put("text", text)
                  put("model_id", MODEL_ID)
                  put(
                      "voice_settings",
                      JSONObject().apply {
                        put("stability", 0.4)
                        put("similarity_boost", 0.75)
                        put("style", 0.55)
                        put("use_speaker_boost", true)
                      },
                  )
                }
                .toString()
        val req =
            Request.Builder()
                .url("$BASE_URL$voiceId")
                .post(body.toRequestBody(JSON))
                .header("xi-api-key", apiKey)
                .header("accept", "audio/mpeg")
                .header("content-type", "application/json")
                .build()
        try {
          client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
              Log.e(TAG, "ElevenLabs ${resp.code}: ${resp.body?.string()?.take(500)}")
              return@withContext null
            }
            resp.body?.bytes()
          }
        } catch (e: IOException) {
          Log.e(TAG, "ElevenLabs request failed", e)
          null
        }
      }
}

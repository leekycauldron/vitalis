package com.vitalis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val TAG = "Vitalis:SpeechRecognizerWrapper"

class SpeechRecognitionUnavailable : Exception("Speech recognition is not available on this device")

class SpeechRecognizerWrapper(private val context: Context) {

  private val _partial = MutableStateFlow("")
  /** Live partial transcript updates; resets at the start of each [recognize] call. */
  val partialTranscript: StateFlow<String> = _partial.asStateFlow()

  /**
   * Records a single utterance and returns the final transcript. Returns null if the user said
   * nothing recognizable. Throws [SpeechRecognitionUnavailable] when STT isn't supported.
   *
   * Must be called from a coroutine; the SpeechRecognizer itself is created and torn down on the
   * main thread.
   */
  suspend fun recognize(): String? {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      throw SpeechRecognitionUnavailable()
    }
    _partial.value = ""

    return withContext(Dispatchers.Main) {
      val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
      val intent =
          Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
          }

      try {
        suspendCancellableCoroutine<String?> { cont ->
          val listener =
              object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                  Log.d(TAG, "onReadyForSpeech")
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                  Log.d(TAG, "onEndOfSpeech")
                }

                override fun onError(error: Int) {
                  Log.w(TAG, "onError code=$error (${errorName(error)})")
                  if (cont.isActive) cont.resume(null)
                }

                override fun onResults(results: Bundle?) {
                  val matches =
                      results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                  val best = matches?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                  Log.d(TAG, "onResults: '$best'")
                  if (cont.isActive) cont.resume(best)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                  val matches =
                      partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                  val text = matches?.firstOrNull().orEmpty()
                  _partial.value = text
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
              }

          cont.invokeOnCancellation {
            runCatching {
              recognizer.cancel()
              recognizer.destroy()
            }
          }

          recognizer.setRecognitionListener(listener)
          recognizer.startListening(intent)
        }
      } finally {
        runCatching { recognizer.destroy() }
      }
    }
  }

  private fun errorName(code: Int): String =
      when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_SERVER -> "SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "SPEECH_TIMEOUT"
        else -> "UNKNOWN($code)"
      }
}

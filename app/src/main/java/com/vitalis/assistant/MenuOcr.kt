package com.vitalis.assistant

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "Vitalis:MenuOcr"

data class OcrLine(val text: String, val box: Rect)

object MenuOcr {

  private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

  suspend fun extract(bitmap: Bitmap): List<OcrLine> {
    val image = InputImage.fromBitmap(bitmap, 0)
    return suspendCancellableCoroutine { cont ->
      recognizer
          .process(image)
          .addOnSuccessListener { result ->
            val lines = buildList {
              for (block in result.textBlocks) {
                for (line in block.lines) {
                  val box = line.boundingBox ?: continue
                  val text = line.text.trim()
                  if (text.isNotEmpty()) add(OcrLine(text, box))
                }
              }
            }
            Log.d(TAG, "OCR extracted ${lines.size} lines")
            cont.resume(lines)
          }
          .addOnFailureListener { e ->
            Log.e(TAG, "OCR failed", e)
            cont.resumeWithException(e)
          }
    }
  }
}

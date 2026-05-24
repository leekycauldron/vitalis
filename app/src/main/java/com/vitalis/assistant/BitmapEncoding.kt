package com.vitalis.assistant

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

object BitmapEncoding {

  fun toBase64Jpeg(bitmap: Bitmap, maxDim: Int = 1024, quality: Int = 80): String {
    val scaled = resizeWithinMaxDim(bitmap, maxDim)
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    if (scaled !== bitmap) scaled.recycle()
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
  }

  private fun resizeWithinMaxDim(bitmap: Bitmap, maxDim: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val largest = maxOf(w, h)
    if (largest <= maxDim) return bitmap
    val scale = maxDim.toFloat() / largest
    return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
  }
}

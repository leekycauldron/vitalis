package com.vitalis.assistant

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "Vitalis:AudioPlayback"
private const val DING_ASSET = "ding.mp3"

class AudioPlayback(private val context: Context) {

  private val playLock = Mutex()
  private val dingAssetExists: Boolean by lazy {
    runCatching { context.assets.openFd(DING_ASSET).close(); true }.getOrDefault(false)
  }

  /** Short notification chime — uses assets/ding.mp3 if present, else the system notification. */
  suspend fun playDing() = playLock.withLock {
    val mp = MediaPlayer()
    try {
      if (dingAssetExists) {
        context.assets.openFd(DING_ASSET).use { afd ->
          mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        }
      } else {
        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mp.setDataSource(context, uri)
      }
      awaitPlay(mp)
    } catch (e: Exception) {
      Log.e(TAG, "playDing failed", e)
    } finally {
      runCatching { mp.release() }
    }
  }

  /** Plays raw MP3 bytes (e.g. from ElevenLabs). Serializes against [playDing]. */
  suspend fun playTtsBytes(bytes: ByteArray) = playLock.withLock {
    val cacheFile = File(context.cacheDir, "vitalis_tts_${System.currentTimeMillis()}.mp3")
    try {
      FileOutputStream(cacheFile).use { it.write(bytes) }
      val mp = MediaPlayer()
      try {
        mp.setDataSource(cacheFile.absolutePath)
        awaitPlay(mp)
      } finally {
        runCatching { mp.release() }
      }
    } catch (e: Exception) {
      Log.e(TAG, "playTtsBytes failed", e)
    } finally {
      runCatching { cacheFile.delete() }
    }
  }

  private suspend fun awaitPlay(mp: MediaPlayer) {
    suspendCancellableCoroutine<Unit> { cont ->
      mp.setOnCompletionListener {
        if (cont.isActive) cont.resume(Unit)
      }
      mp.setOnErrorListener { _, what, extra ->
        Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
        if (cont.isActive) cont.resume(Unit)
        true
      }
      cont.invokeOnCancellation { runCatching { mp.stop() } }
      mp.prepare()
      mp.start()
    }
  }
}

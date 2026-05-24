package com.vitalis.placesearch

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "Vitalis:LocationProvider"

class LocationProvider(private val context: Context) {

  private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

  fun hasPermission(): Boolean {
    val coarse =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    val fine =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    return coarse || fine
  }

  /** Returns the device's current location, or null if unavailable / permission missing. */
  @SuppressLint("MissingPermission")
  suspend fun currentLocation(): Location? {
    if (!hasPermission()) {
      Log.w(TAG, "currentLocation: no location permission")
      return null
    }
    // Try the cheap cached fix first.
    val cached = lastLocationOrNull()
    if (cached != null) return cached
    return requestFresh()
  }

  @SuppressLint("MissingPermission")
  private suspend fun lastLocationOrNull(): Location? =
      suspendCancellableCoroutine { cont ->
        client
            .lastLocation
            .addOnSuccessListener { loc ->
              if (cont.isActive) cont.resume(loc)
            }
            .addOnFailureListener { e ->
              Log.w(TAG, "lastLocation failed", e)
              if (cont.isActive) cont.resume(null)
            }
      }

  @SuppressLint("MissingPermission")
  private suspend fun requestFresh(): Location? {
    val cts = CancellationTokenSource()
    return suspendCancellableCoroutine { cont ->
      cont.invokeOnCancellation { cts.cancel() }
      client
          .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
          .addOnSuccessListener { loc ->
            if (cont.isActive) cont.resume(loc)
          }
          .addOnFailureListener { e ->
            Log.w(TAG, "getCurrentLocation failed", e)
            if (cont.isActive) cont.resume(null)
          }
    }
  }
}

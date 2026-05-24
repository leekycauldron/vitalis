package com.vitalis.foodlog

import android.content.Context
import android.util.Log

private const val TAG = "Vitalis:FoodsCsvLoader"
private const val ASSET_PATH = "foods.csv"

object FoodsCsvLoader {

  @Volatile private var cached: Map<String, FoodInfo>? = null

  /** Returns label-lowercased → FoodInfo. Loaded once per process. */
  fun load(context: Context): Map<String, FoodInfo> {
    cached?.let {
      return it
    }
    synchronized(this) {
      cached?.let {
        return it
      }
      val parsed = parse(context)
      cached = parsed
      return parsed
    }
  }

  private fun parse(context: Context): Map<String, FoodInfo> {
    return try {
      context.assets.open(ASSET_PATH).bufferedReader().useLines { lines ->
        lines
            .drop(1) // header row
            .mapNotNull { line ->
              val parts = line.split(",")
              if (parts.size < 6) {
                Log.w(TAG, "Skipping malformed row: $line")
                return@mapNotNull null
              }
              runCatching {
                    FoodInfo(
                        label = parts[0].trim(),
                        displayName = parts[1].trim(),
                        calories = parts[2].trim().toInt(),
                        proteinG = parts[3].trim().toDouble(),
                        carbsG = parts[4].trim().toDouble(),
                        fatG = parts[5].trim().toDouble(),
                    )
                  }
                  .onFailure { Log.w(TAG, "Skipping unparseable row: $line", it) }
                  .getOrNull()
            }
            .associateBy { it.label.lowercase() }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load $ASSET_PATH", e)
      emptyMap()
    }
  }
}

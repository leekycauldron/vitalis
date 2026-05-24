package com.vitalis.foodlog

import android.content.Context
import android.util.Log
import com.vitalis.foodlog.db.FoodLogDao
import com.vitalis.foodlog.db.FoodLogEntity
import com.vitalis.foodlog.db.VitalisDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "Vitalis:FoodLogRepo"

private const val DEDUP_WINDOW_MS = 60_000L
private const val RECENT_WINDOW_MS = 120_000L

private val STOPWORDS =
    setOf(
        "a", "an", "the", "some", "my", "your", "and", "or", "of", "with", "in", "on",
        "cup", "cups", "mug", "mugs", "glass", "glasses", "bottle", "bottles",
        "bowl", "bowls", "plate", "plates", "dish", "dishes",
        "hot", "cold", "iced", "warm", "fresh", "frozen",
        "small", "medium", "large", "regular", "single", "double", "triple",
        "slice", "slices", "piece", "pieces", "serving", "servings", "portion",
    )

internal fun normalizeFoodKey(raw: String): String {
  val collapsed =
      raw.lowercase()
          .replace(Regex("[^a-z0-9 ]"), " ")
          .split(Regex("\\s+"))
          .filter { it.isNotBlank() && it !in STOPWORDS }
          .joinToString(" ")
  return collapsed.ifBlank { raw.lowercase().trim() }
}

/**
 * Detection returned by the food classifier — pre-DB shape. `label` may be null if the model didn't
 * match it to a known asset entry.
 */
data class FoodDetection(
    val label: String?,
    val name: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val isJunk: Boolean,
    val wasEstimated: Boolean,
)

class FoodLogRepository(private val dao: FoodLogDao) {

  private val cooldown = mutableMapOf<String, Long>()
  private val cooldownLock = Mutex()

  fun observeRecent(limit: Int = 100): Flow<List<FoodLogEntity>> = dao.observeRecent(limit)

  /**
   * Returns true if a new entry was persisted, false if the detection was deduped against a recent
   * log of the same label/name.
   */
  suspend fun tryLog(detection: FoodDetection, now: Long = System.currentTimeMillis()): Boolean {
    val key = dedupKey(detection)
    cooldownLock.withLock {
      val last = cooldown[key]
      if (last != null && now - last < DEDUP_WINDOW_MS) {
        Log.d(
            TAG,
            "DEDUPED '${detection.name}' (key='$key', logged ${now - last}ms ago)",
        )
        return false
      }
      cooldown[key] = now
    }
    Log.d(TAG, "LOGGED '${detection.name}' (key='$key', junk=${detection.isJunk})")
    dao.insert(
        FoodLogEntity(
            timestamp = now,
            label = detection.label,
            name = detection.name,
            calories = detection.calories,
            proteinG = detection.proteinG,
            carbsG = detection.carbsG,
            fatG = detection.fatG,
            isJunk = detection.isJunk,
            wasEstimated = detection.wasEstimated,
        )
    )
    return true
  }

  /** Labels seen within [RECENT_WINDOW_MS] — passed back to the model as a prompt hint. */
  suspend fun recentlySeenLabels(now: Long = System.currentTimeMillis()): List<String> {
    return cooldownLock.withLock {
      cooldown.entries
          .filter { (_, ts) -> now - ts < RECENT_WINDOW_MS }
          .map { it.key }
          .sorted()
    }
  }

  /** Clears in-session dedup state. Persisted entries are untouched. */
  suspend fun resetSession() {
    cooldownLock.withLock { cooldown.clear() }
  }

  /** Wipes every entry from the database AND resets in-memory dedup state. Irreversible. */
  suspend fun clearAll() {
    dao.clear()
    cooldownLock.withLock { cooldown.clear() }
    Log.d(TAG, "Cleared all food log entries")
  }

  private fun dedupKey(detection: FoodDetection): String =
      normalizeFoodKey(detection.label ?: detection.name)

  companion object {
    fun create(context: Context): FoodLogRepository =
        FoodLogRepository(VitalisDatabase.get(context).foodLogDao())
  }
}

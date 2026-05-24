package com.vitalis.profile

import com.vitalis.foodlog.FoodLogSummary
import java.util.Calendar

enum class Severity { INFO, WARN }

data class ImbalanceFlag(val severity: Severity, val text: String)

data class MacroBalance(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val targets: MacroTargets,
    val hourOfDay: Int,
    val imbalances: List<ImbalanceFlag>,
) {
  val calPct: Int = pct(calories, targets.calories)
  val proteinPct: Int = pct(proteinG, targets.proteinG)
  val carbsPct: Int = pct(carbsG, targets.carbsG)
  val fatPct: Int = pct(fatG, targets.fatG)

  /** Compact one-liner suitable for prompt injection. */
  val summaryLine: String =
      "${calories} of ${targets.calories} kcal (${calPct}%), " +
          "P ${proteinG}/${targets.proteinG}g (${proteinPct}%), " +
          "C ${carbsG}/${targets.carbsG}g (${carbsPct}%), " +
          "F ${fatG}/${targets.fatG}g (${fatPct}%) — at ${hourOfDay}:00"

  companion object {
    private fun pct(v: Int, target: Int): Int =
        if (target <= 0) 0 else ((v.toDouble() / target.toDouble()) * 100).toInt()

    /** Hour-prorated expectation: at 12pm we expect ~50% consumed, at 6pm ~75%, etc. */
    private fun expectedPctByHour(hour: Int): Double {
      // Eaten between ~8am and ~9pm; linearly ramp.
      val start = 8.0
      val end = 21.0
      return ((hour.coerceIn(0, 23) - start) / (end - start)).coerceIn(0.0, 1.0)
    }

    fun compute(summary: FoodLogSummary, targets: MacroTargets, nowMs: Long = System.currentTimeMillis()): MacroBalance {
      val hour = Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)
      val expected = expectedPctByHour(hour) // 0..1

      val cal = summary.totalCalories
      val protein = summary.totalProteinG.toInt()
      val carbs = summary.totalCarbsG.toInt()
      val fat = summary.totalFatG.toInt()

      val flags = mutableListOf<ImbalanceFlag>()

      // 1) Macro overshoot vs. expected window
      val expectedCarbs = targets.carbsG * expected
      if (expectedCarbs > 0 && carbs > expectedCarbs * 1.10) {
        flags +=
            ImbalanceFlag(
                Severity.WARN,
                "Carbs are running hot — ${carbs}g eaten vs. ~${expectedCarbs.toInt()}g expected by this hour.",
            )
      }
      val expectedFat = targets.fatG * expected
      if (expectedFat > 0 && fat > expectedFat * 1.15) {
        flags +=
            ImbalanceFlag(
                Severity.WARN,
                "Fat intake outpacing target — ${fat}g vs. ~${expectedFat.toInt()}g expected.",
            )
      }

      // 2) Protein shortfall vs. expected
      val expectedProtein = targets.proteinG * expected
      if (expected > 0.2 && expectedProtein > 0 && protein < expectedProtein * 0.6) {
        flags +=
            ImbalanceFlag(
                Severity.WARN,
                "Protein is low — ${protein}g vs. ~${expectedProtein.toInt()}g expected; only ${pct(protein, targets.proteinG)}% of daily target.",
            )
      }

      // 3) Junk creep
      if (summary.junkCount >= 2) {
        flags +=
            ImbalanceFlag(
                Severity.WARN,
                "${summary.junkCount} junk-flagged items logged today already.",
            )
      }

      // 4) Calorie overshoot
      if (cal > targets.calories) {
        flags +=
            ImbalanceFlag(
                Severity.WARN,
                "You're over today's calorie target (${cal}/${targets.calories} kcal).",
            )
      }

      return MacroBalance(
          calories = cal,
          proteinG = protein,
          carbsG = carbs,
          fatG = fat,
          targets = targets,
          hourOfDay = hour,
          imbalances = flags,
      )
    }
  }
}

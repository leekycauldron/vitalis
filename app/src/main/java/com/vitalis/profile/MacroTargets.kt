package com.vitalis.profile

import kotlin.math.roundToInt

data class MacroTargets(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fibreG: Int,
) {
  companion object {
    val Default = MacroTargets(calories = 2000, proteinG = 150, carbsG = 200, fatG = 65, fibreG = 30)

    /** Mifflin–St Jeor BMR + activity multiplier; macro split by goal. */
    fun fromProfile(p: UserProfile): MacroTargets {
      val age = p.age ?: return Default
      val height = p.heightCm ?: return Default
      val weight = p.weightKg ?: return Default
      val gender = p.gender ?: return Default

      val bmr =
          when (gender) {
            Gender.MALE -> 10 * weight + 6.25 * height - 5 * age + 5
            Gender.FEMALE -> 10 * weight + 6.25 * height - 5 * age - 161
            Gender.OTHER -> 10 * weight + 6.25 * height - 5 * age - 78
          }
      val mult = (p.activityLevel ?: ActivityLevel.MODERATE).multiplier
      val tdee = (bmr * mult).roundToInt()

      val goal = p.primaryGoal ?: PrimaryGoal.IMPROVE_ENERGY
      val (calAdj, split) =
          when (goal) {
            PrimaryGoal.LOSE_WEIGHT -> -400 to Triple(0.35, 0.35, 0.30)
            PrimaryGoal.BUILD_MUSCLE -> 250 to Triple(0.35, 0.40, 0.25)
            PrimaryGoal.IMPROVE_ENERGY -> 0 to Triple(0.30, 0.45, 0.25)
            PrimaryGoal.BETTER_SLEEP -> 0 to Triple(0.30, 0.40, 0.30)
            PrimaryGoal.LONGEVITY -> -100 to Triple(0.25, 0.45, 0.30)
          }
      val cal = (tdee + calAdj).coerceAtLeast(1200)
      val protein = (cal * split.first / 4.0).roundToInt()
      val carbs = (cal * split.second / 4.0).roundToInt()
      val fat = (cal * split.third / 9.0).roundToInt()
      val fibre = (cal / 1000.0 * 14.0).roundToInt() // dietary guideline 14g per 1000 kcal
      return MacroTargets(calories = cal, proteinG = protein, carbsG = carbs, fatG = fat, fibreG = fibre)
    }
  }
}

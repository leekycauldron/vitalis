package com.vitalis.profile

enum class Gender(val label: String) { MALE("Male"), FEMALE("Female"), OTHER("Other") }

enum class PrimaryGoal(val label: String) {
  IMPROVE_ENERGY("Improve energy"),
  LOSE_WEIGHT("Lose weight"),
  BUILD_MUSCLE("Build muscle"),
  BETTER_SLEEP("Better sleep"),
  LONGEVITY("Longevity"),
}

enum class ActivityLevel(val label: String, val multiplier: Double) {
  SEDENTARY("Sedentary", 1.2),
  LIGHT("Lightly active", 1.375),
  MODERATE("Moderately active", 1.55),
  ACTIVE("Active", 1.725),
  VERY_ACTIVE("Very active", 1.9),
}

enum class FocusArea(val label: String, val colorHex: Long) {
  ENERGY("Energy", 0xFFF59E0B),
  SLEEP("Sleep", 0xFF3B82F6),
  NUTRITION("Nutrition", 0xFF10B981),
  FITNESS("Fitness", 0xFF22C55E),
  MOOD("Mood", 0xFF8B5CF6),
  LONGEVITY("Longevity", 0xFFEC4899),
}

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val age: Int? = null,
    val gender: Gender? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val primaryGoal: PrimaryGoal? = null,
    val activityLevel: ActivityLevel? = null,
    val focusAreas: Set<FocusArea> = emptySet(),
    val dnaNotes: String = "",
    val freeFormNotes: String = "",
    val onboardingComplete: Boolean = false,
) {
  /** Short demographic line used in prompts: "32y male, 180cm 78kg, moderately active". */
  fun summary(): String {
    val parts = mutableListOf<String>()
    age?.let { parts += "${it}y" }
    gender?.let { parts += it.label.lowercase() }
    val anthro = listOfNotNull(heightCm?.let { "${it}cm" }, weightKg?.let { "${it.toInt()}kg" })
    if (anthro.isNotEmpty()) parts += anthro.joinToString(" ")
    activityLevel?.let { parts += it.label.lowercase() }
    return if (parts.isEmpty()) "(no demographics)" else parts.joinToString(", ")
  }
}

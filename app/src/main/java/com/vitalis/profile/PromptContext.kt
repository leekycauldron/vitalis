package com.vitalis.profile

/**
 * Single source of truth for the model-facing context blurb. Every Anthropic call that previously
 * took `personalProfile` / `dietaryAvoidance` text now takes a [PromptContext] and embeds
 * [toSystemBlurb].
 */
data class PromptContext(
    val profile: UserProfile,
    val macroBalance: MacroBalance,
    /** Recently-detected food labels — used only by the food-detector to discourage re-logging. */
    val recentLabels: List<String> = emptyList(),
) {

  fun toSystemBlurb(): String {
    val name = profile.name.ifBlank { "the user" }
    val goals = profile.primaryGoal?.label ?: "(none set)"
    val focus =
        if (profile.focusAreas.isEmpty()) "(none)"
        else profile.focusAreas.joinToString(", ") { it.label.lowercase() }
    val dna = profile.dnaNotes.ifBlank { "(none provided)" }
    val notes = profile.freeFormNotes.ifBlank { "(none)" }
    val flags =
        if (macroBalance.imbalances.isEmpty()) "(no notable imbalances)"
        else macroBalance.imbalances.joinToString("\n") { "- " + it.text }

    return """
        Profile of $name: ${profile.summary()}.
        Goal: $goals. Focus areas: $focus.
        DNA notes: $dna
        Additional context: $notes
        Today so far: ${macroBalance.summaryLine}.
        Current imbalances / observations:
        $flags
    """.trimIndent()
  }
}

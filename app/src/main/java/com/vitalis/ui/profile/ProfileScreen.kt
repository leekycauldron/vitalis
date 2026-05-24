package com.vitalis.ui.profile

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitalis.BuildConfig
import com.vitalis.profile.ActivityLevel
import com.vitalis.profile.Gender
import com.vitalis.profile.MacroTargets
import com.vitalis.profile.PrimaryGoal
import com.vitalis.profile.ProfileViewModel
import com.vitalis.settings.SettingsViewModel
import com.vitalis.ui.common.SectionLabel
import com.vitalis.ui.common.VCard
import com.vitalis.ui.common.Wordmark
import com.vitalis.ui.onboarding.VInput
import com.vitalis.ui.theme.VColors

@Composable
fun ProfileScreen(
    onOpenGenetic: () -> Unit,
    onOpenGlasses: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel =
        viewModel(factory = ProfileViewModel.Factory((LocalActivity.current as ComponentActivity).application)),
    settingsViewModel: SettingsViewModel =
        viewModel(factory = SettingsViewModel.Factory((LocalActivity.current as ComponentActivity).application)),
) {
  val profile by profileViewModel.profile.collectAsStateWithLifecycle()
  val foodCount by settingsViewModel.foodLogEntryCount.collectAsStateWithLifecycle()
  var showClear by remember { mutableStateOf(false) }
  var showReset by remember { mutableStateOf(false) }

  val targets = MacroTargets.fromProfile(profile)

  Column(
      modifier =
          modifier.fillMaxSize().background(VColors.Bg).statusBarsPadding()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Wordmark()
      Text(text = "Profile", color = VColors.InkMd, style = MaterialTheme.typography.bodyMedium)
    }

    // Top profile chip
    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = profile.name.ifBlank { "(unnamed)" },
            color = VColors.Ink,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = profile.email.ifBlank { "(no email)" }, color = VColors.InkMd, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = profile.summary(),
            color = VColors.InkLo,
            style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    // Macro targets card
    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("DAILY TARGETS")
        Text(
            text = "${targets.calories} kcal · ${targets.proteinG}g P · ${targets.carbsG}g C · ${targets.fatG}g F · ${targets.fibreG}g fibre",
            color = VColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Derived from your profile. Edit fields below to recalculate.",
            color = VColors.InkLo,
            style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    // Edit profile
    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("ABOUT YOU")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          VInput(
              value = profile.name,
              onValueChange = { v -> profileViewModel.update { it.copy(name = v) } },
              label = "Name",
              modifier = Modifier.weight(1f),
          )
          VInput(
              value = profile.age?.toString() ?: "",
              onValueChange = { v -> profileViewModel.update { it.copy(age = v.toIntOrNull()) } },
              label = "Age",
              modifier = Modifier.weight(1f),
              keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          VInput(
              value = profile.heightCm?.toString() ?: "",
              onValueChange = { v -> profileViewModel.update { it.copy(heightCm = v.toIntOrNull()) } },
              label = "Height (cm)",
              modifier = Modifier.weight(1f),
              keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
          )
          VInput(
              value = profile.weightKg?.toString() ?: "",
              onValueChange = { v -> profileViewModel.update { it.copy(weightKg = v.toDoubleOrNull()) } },
              label = "Weight (kg)",
              modifier = Modifier.weight(1f),
              keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
          )
        }
        EnumRow(
            label = "Gender",
            options = Gender.entries.toTypedArray(),
            selected = profile.gender,
            displayOf = { it.label },
            onSelect = { v -> profileViewModel.update { p -> p.copy(gender = v) } },
        )
        EnumRow(
            label = "Goal",
            options = PrimaryGoal.entries.toTypedArray(),
            selected = profile.primaryGoal,
            displayOf = { it.label },
            onSelect = { v -> profileViewModel.update { p -> p.copy(primaryGoal = v) } },
        )
        EnumRow(
            label = "Activity",
            options = ActivityLevel.entries.toTypedArray(),
            selected = profile.activityLevel,
            displayOf = { it.label },
            onSelect = { v -> profileViewModel.update { p -> p.copy(activityLevel = v) } },
        )
        VInput(
            value = profile.freeFormNotes,
            onValueChange = { v -> profileViewModel.update { it.copy(freeFormNotes = v) } },
            label = "Additional context (allergies, restrictions, preferences)",
            singleLine = false,
        )
      }
    }

    // Genetics shortcut
    VCard(onClick = onOpenGenetic, accent = VColors.Purple) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "🧬", style = MaterialTheme.typography.headlineMedium)
        Column(modifier = Modifier.weight(1f)) {
          Text(text = "Genetic profile", color = VColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text(
              text =
                  if (profile.dnaNotes.isBlank()) "Add DNA notes to deepen recommendations."
                  else "${profile.dnaNotes.length} chars of notes saved",
              color = VColors.InkMd,
              style = MaterialTheme.typography.bodySmall,
          )
        }
        Text(text = "›", color = VColors.PurpleL, style = MaterialTheme.typography.titleLarge)
      }
    }

    // Glasses & API
    VCard(onClick = onOpenGlasses) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("GLASSES & APIs")
        KeyRow(label = "Anthropic", present = BuildConfig.ANTHROPIC_API_KEY.isNotBlank())
        KeyRow(label = "Pexels", present = BuildConfig.PEXELS_API_KEY.isNotBlank())
        KeyRow(label = "ElevenLabs", present = BuildConfig.ELEVENLABS_API_KEY.isNotBlank())
        KeyRow(label = "ElevenLabs voice", present = BuildConfig.ELEVENLABS_VOICE_ID.isNotBlank())
        KeyRow(label = "Google Places", present = BuildConfig.GOOGLE_PLACES_API_KEY.isNotBlank())
        Text(
            text = "Tap to open assistant mode →",
            color = VColors.PurpleL,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
      }
    }

    // Data card
    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("DATA")
        Text(
            text = "$foodCount entr${if (foodCount == 1) "y" else "ies"} in food log",
            color = VColors.InkMd,
            style = MaterialTheme.typography.bodySmall,
        )
        DangerRow(text = "Clear food log", onClick = { showClear = true }, enabled = foodCount > 0)
        DangerRow(text = "Reset profile (re-runs onboarding)", onClick = { showReset = true })
      }
    }

    Text(
        text = "Vitalis · Demo · v0.1",
        color = VColors.InkLo,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(vertical = 8.dp),
    )
    Spacer(modifier = Modifier.height(8.dp))
  }

  if (showClear) {
    AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("Clear food log?") },
        text = { Text("Deletes all $foodCount logged entr${if (foodCount == 1) "y" else "ies"} from this device. Can't be undone.") },
        confirmButton = {
          TextButton(onClick = {
            settingsViewModel.clearFoodLog()
            showClear = false
          }) { Text("Clear", color = VColors.Red) }
        },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("Cancel") } },
    )
  }
  if (showReset) {
    AlertDialog(
        onDismissRequest = { showReset = false },
        title = { Text("Reset profile?") },
        text = { Text("Wipes your name, goals, focus areas and DNA notes. Food log is unaffected.") },
        confirmButton = {
          TextButton(onClick = {
            profileViewModel.resetProfile()
            showReset = false
          }) { Text("Reset", color = VColors.Red) }
        },
        dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } },
    )
  }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun <T> EnumRow(
    label: String,
    options: Array<T>,
    selected: T?,
    displayOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    SectionLabel(label.uppercase())
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
      options.forEach { opt ->
        val active = selected == opt
        Box(
            modifier =
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (active) VColors.Purple else VColors.Card2)
                    .border(1.dp, if (active) VColors.Purple else VColors.Border, RoundedCornerShape(999.dp))
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
          Text(
              text = displayOf(opt),
              color = if (active) Color.White else VColors.InkMd,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Medium,
          )
        }
      }
    }
  }
}

@Composable
private fun KeyRow(label: String, present: Boolean) {
  val color = if (present) VColors.Teal else VColors.Red
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(99.dp)).background(color))
    Text(
        text = "$label: ${if (present) "configured" else "missing"}",
        color = if (present) VColors.Ink else VColors.InkMd,
        style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun DangerRow(text: String, onClick: () -> Unit, enabled: Boolean = true) {
  Row(
      modifier =
          Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp))
              .background(if (enabled) Color(0x33EF4444) else Color(0x14EF4444))
              .clickable(enabled = enabled, onClick = onClick)
              .padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = text, color = if (enabled) VColors.Red else VColors.Red.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
  }
}

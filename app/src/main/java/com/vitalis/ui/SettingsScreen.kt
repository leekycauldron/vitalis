package com.vitalis.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitalis.BuildConfig
import com.vitalis.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel =
        viewModel(
            factory =
                SettingsViewModel.Factory(
                    (LocalActivity.current as ComponentActivity).application
                )
        ),
) {
  val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
  val foodLogCount by settingsViewModel.foodLogEntryCount.collectAsStateWithLifecycle()
  var showClearConfirm by remember { mutableStateOf(false) }

  MaterialTheme(colorScheme = darkColorScheme()) {
    Box(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
      Column(
          modifier =
              Modifier.fillMaxSize()
                  .verticalScroll(rememberScrollState())
                  .statusBarsPadding()
                  .navigationBarsPadding(),
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
          IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
          Text(
              text = "Settings",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
              modifier = Modifier.align(Alignment.Center),
          )
        }

        Text(
            text = "Personal profile",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Text(
            text =
                "Free-text for now. Mention goals, weight, height, allergies, dietary preferences — anything that should shape recommendations.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        OutlinedTextField(
            value = settings.personalProfile,
            onValueChange = settingsViewModel::setPersonalProfile,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            placeholder = { Text("e.g. 28y, 75kg, 180cm. Cutting weight. Avoid red meat. Lactose intolerant.") },
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "What to avoid",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Text(
            text =
                "Short list of things you're trying to cut. Used by the food logger to flag junk hits and trigger a sarcastic nudge.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        OutlinedTextField(
            value = settings.dietaryAvoidance,
            onValueChange = settingsViewModel::setDietaryAvoidance,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("e.g. added sugar, fried foods, alcohol") },
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "API keys", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(
            text =
                "Keys are loaded at build time from local.properties. Edit that file and rebuild to change them.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        KeyStatusRow(label = "Anthropic", present = BuildConfig.ANTHROPIC_API_KEY.isNotBlank())
        KeyStatusRow(label = "Pexels", present = BuildConfig.PEXELS_API_KEY.isNotBlank())
        KeyStatusRow(label = "ElevenLabs", present = BuildConfig.ELEVENLABS_API_KEY.isNotBlank())
        KeyStatusRow(label = "ElevenLabs voice ID", present = BuildConfig.ELEVENLABS_VOICE_ID.isNotBlank())

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Food log", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(
            text =
                if (foodLogCount == 0) "No entries logged yet."
                else "$foodLogCount entr${if (foodLogCount == 1) "y" else "ies"} stored on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        Button(
            onClick = { showClearConfirm = true },
            enabled = foodLogCount > 0,
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B0000),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF333333),
                    disabledContentColor = Color.White.copy(alpha = 0.4f),
                ),
        ) {
          Icon(Icons.Default.DeleteSweep, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Clear food log")
        }
      }
    }
  }

  if (showClearConfirm) {
    AlertDialog(
        onDismissRequest = { showClearConfirm = false },
        title = { Text("Clear food log?") },
        text = {
          Text(
              "This deletes all $foodLogCount logged entr${if (foodLogCount == 1) "y" else "ies"} from this device. This can't be undone."
          )
        },
        confirmButton = {
          TextButton(
              onClick = {
                settingsViewModel.clearFoodLog()
                showClearConfirm = false
              }
          ) {
            Text("Clear", color = Color(0xFFFF6B6B))
          }
        },
        dismissButton = {
          TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
        },
    )
  }
}

@Composable
private fun KeyStatusRow(label: String, present: Boolean) {
  val (status, color) =
      if (present) "configured" to Color(0xFF7DD3A0) else "missing" to Color(0xFFFF8A8A)
  Text(
      text = "$label: $status",
      color = color,
      style = MaterialTheme.typography.bodyMedium,
  )
}

package com.vitalis.ui.home

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitalis.ui.common.ProgressRing
import com.vitalis.ui.common.SectionLabel
import com.vitalis.ui.common.VCard
import com.vitalis.ui.theme.VColors
import com.vitalis.wearables.WearablesUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    wearablesState: WearablesUiState,
    onOpenGenetic: () -> Unit,
    onOpenManualAdd: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel =
        viewModel(factory = HomeViewModel.Factory((LocalActivity.current as ComponentActivity).application)),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val today = remember1 { SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()) }
  val nameDisplay = state.profile.name.ifBlank { "Friend" }

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(VColors.Bg)
              .statusBarsPadding()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 18.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Greeting row
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
            text = "Hi, ${nameDisplay.split(" ").first()}",
            color = VColors.Ink,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = today, color = VColors.InkMd, style = MaterialTheme.typography.bodyMedium)
      }
      Box(
          modifier =
              Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                  .background(Color(0x0AFFFFFF))
                  .border(1.dp, VColors.Border, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center,
      ) {
        Text(text = "🔔", color = VColors.Ink)
      }
    }

    // Progress rings row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      ProgressRing(
          value = state.summary.totalCalories,
          max = state.targets.calories,
          label = "Calories",
          color = VColors.Purple,
          unit = "kcal",
      )
      ProgressRing(
          value = state.summary.totalProteinG.toInt(),
          max = state.targets.proteinG,
          label = "Protein",
          color = VColors.Teal,
          unit = "g",
      )
      ProgressRing(
          value = 0,
          max = 8,
          label = "Sleep",
          color = VColors.Blue,
          unit = "hr",
      )
      ProgressRing(
          value = 0,
          max = 10000,
          label = "Steps",
          color = VColors.Amber,
      )
    }

    // Top insight
    TopInsightCard(text = topInsightText(state))

    // Glasses live-status pill (passive — Assistant tab launches the flow)
    GlassesStatusCard(wearablesState = wearablesState)

    // 2x2 quick nav
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickNav(label = "Nutrition", emoji = "🥗", modifier = Modifier.weight(1f))
        QuickNav(label = "Sleep", emoji = "🌙", modifier = Modifier.weight(1f))
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickNav(label = "Activity", emoji = "💪", modifier = Modifier.weight(1f))
        QuickNav(label = "Mood", emoji = "🧘", modifier = Modifier.weight(1f))
      }
    }

    // Genetics banner
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF2D1B5C), Color(0xFF4C1D95)))
                )
                .border(1.dp, VColors.BorderPurple, RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenGenetic)
                .padding(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "🧬", style = MaterialTheme.typography.headlineMedium)
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = "Genetic profile",
              color = VColors.Ink,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
          )
          Text(
              text =
                  if (state.profile.dnaNotes.isBlank()) "Add DNA notes to deepen recommendations."
                  else "Notes saved · used to personalise picks.",
              color = VColors.InkMd,
              style = MaterialTheme.typography.bodySmall,
          )
        }
        Text(text = "›", color = VColors.PurpleL, style = MaterialTheme.typography.titleLarge)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
  }
}

private fun topInsightText(state: HomeDashboardState): String {
  if (state.balance.imbalances.isNotEmpty()) return state.balance.imbalances.first().text
  if (state.summary.entryCount == 0)
    return "No meals logged yet today. Use the + button or your glasses to start."
  return "You're on track. ${state.balance.proteinPct}% of protein, ${state.balance.carbsPct}% of carbs."
}

@Composable
private fun TopInsightCard(text: String) {
  var expanded by remember(text) { mutableStateOf(false) }
  var overflowing by remember(text) { mutableStateOf(false) }
  VCard(accent = VColors.Purple) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      SectionLabel("TOP INSIGHT")
      Text(
          text = text,
          color = VColors.Ink,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = if (expanded) Int.MAX_VALUE else 3,
          overflow = if (expanded) androidx.compose.ui.text.style.TextOverflow.Clip
                     else androidx.compose.ui.text.style.TextOverflow.Ellipsis,
          onTextLayout = { result ->
            // Latch true on the collapsed pass — don't reset to false when expanded shows all lines.
            if (!expanded && result.hasVisualOverflow) overflowing = true
          },
      )
      if (overflowing) {
        Text(
            text = if (expanded) "Show less" else "Read more",
            color = VColors.PurpleL,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { expanded = !expanded },
        )
      }
    }
  }
}

@Composable
private fun GlassesStatusCard(wearablesState: WearablesUiState) {
  val connected = wearablesState.isRegistered && wearablesState.hasActiveDevice
  val dotColor = if (connected) VColors.Teal else VColors.InkLo
  VCard {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Box(
          modifier =
              Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                  .background(if (connected) VColors.Teal.copy(alpha = 0.18f) else Color(0x0AFFFFFF)),
          contentAlignment = Alignment.Center,
      ) {
        Text(text = "👓", style = MaterialTheme.typography.titleLarge)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = "Ray-Ban Meta",
            color = VColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(99.dp)).background(dotColor))
          Text(
              text = if (connected) "Connected" else "Not connected",
              color = if (connected) VColors.Teal else VColors.InkMd,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
          )
        }
      }
    }
  }
}

@Composable
private fun QuickNav(label: String, emoji: String, modifier: Modifier = Modifier) {
  VCard(modifier = modifier, padding = 14.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
      Text(text = label, color = VColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
  }
}

// helper since `remember` requires a key; we want a one-shot side-effect-free value
@Composable
private inline fun <T> remember1(noinline calculation: () -> T): T = androidx.compose.runtime.remember { calculation() }

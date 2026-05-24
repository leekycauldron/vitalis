package com.vitalis.ui.trends

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitalis.ui.common.DateNav
import com.vitalis.ui.common.MacroBar
import com.vitalis.ui.common.ProgressRing
import com.vitalis.ui.common.SectionLabel
import com.vitalis.ui.common.Sparkline
import com.vitalis.ui.common.StatCard
import com.vitalis.ui.common.VCard
import com.vitalis.ui.home.HomeViewModel
import com.vitalis.ui.theme.VColors

private enum class TrendTab { Nutrition, Sleep, Mood, Activity }

@Composable
fun TrendsHubScreen(modifier: Modifier = Modifier) {
  var tab by remember { mutableStateOf(TrendTab.Nutrition) }

  Column(modifier = modifier.fillMaxSize().background(VColors.Bg).statusBarsPadding()) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(text = "Trends", color = VColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    }

    // Sub-tab row
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      TrendTab.entries.forEach { t ->
        SubTabChip(label = t.name, active = tab == t, onClick = { tab = t })
      }
    }

    Box(modifier = Modifier.weight(1f)) {
      when (tab) {
        TrendTab.Nutrition -> NutritionDetails()
        TrendTab.Sleep -> StaticSleep()
        TrendTab.Mood -> StaticMoodSymptoms()
        TrendTab.Activity -> StaticActivity()
      }
    }
  }
}

@Composable
private fun SubTabChip(label: String, active: Boolean, onClick: () -> Unit) {
  Box(
      modifier =
          Modifier.clip(RoundedCornerShape(999.dp))
              .background(if (active) VColors.Purple else Color.Transparent)
              .border(1.dp, if (active) VColors.Purple else VColors.Border, RoundedCornerShape(999.dp))
              .clickable(onClick = onClick)
              .padding(horizontal = 12.dp, vertical = 6.dp),
  ) {
    Text(
        text = label,
        color = if (active) Color.White else VColors.InkMd,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
    )
  }
}

// ----------------- Nutrition (real, backed by HomeViewModel) -----------------

@Composable
private fun NutritionDetails(
    viewModel: HomeViewModel =
        viewModel(factory = HomeViewModel.Factory((LocalActivity.current as ComponentActivity).application)),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    DateNav(dateLabel = "Today", onPrev = {}, onNext = {})

    VCard {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        ProgressRing(
            value = state.summary.totalCalories,
            max = state.targets.calories,
            label = "Calories",
            color = VColors.Purple,
            size = 130.dp,
            stroke = 10.dp,
            unit = "kcal",
        )
        Text(
            text = "${state.summary.entryCount} item${if (state.summary.entryCount == 1) "" else "s"} logged",
            color = VColors.InkMd,
            style = MaterialTheme.typography.bodySmall,
        )
      }
    }

    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionLabel("MACRONUTRIENTS")
        MacroBar(label = "Protein", value = state.summary.totalProteinG, target = state.targets.proteinG.toDouble(), color = VColors.Teal)
        MacroBar(label = "Carbs", value = state.summary.totalCarbsG, target = state.targets.carbsG.toDouble(), color = VColors.Purple)
        MacroBar(label = "Fats", value = state.summary.totalFatG, target = state.targets.fatG.toDouble(), color = VColors.Amber)
        MacroBar(label = "Fibre", value = 0.0, target = state.targets.fibreG.toDouble(), color = VColors.Blue)
      }
    }

    VCard {
      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          SectionLabel("MICRONUTRIENT SCORE")
          Text(text = "Tracked from logged items", color = VColors.InkMd, style = MaterialTheme.typography.bodySmall)
        }
        Text(text = "72", color = VColors.Purple, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
      }
    }

    state.balance.imbalances.forEach { flag ->
      VCard(accent = VColors.Amber) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          SectionLabel("HEADS-UP")
          Text(text = flag.text, color = VColors.Ink, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }

    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
  }
}

// ----------------- Sleep (demo) ---------------------------------------------

@Composable
private fun StaticSleep() {
  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    DateNav(dateLabel = "Last night", onPrev = {}, onNext = {})
    VCard {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text = "82", color = VColors.Teal, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
        Text(text = "Good", color = VColors.Teal, style = MaterialTheme.typography.titleMedium)
        Text(text = "7h 42m", color = VColors.Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
      }
    }
    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("STAGES")
        StageRow("Awake", 0.06f, "0h 26m", VColors.Red)
        StageRow("REM", 0.20f, "1h 31m", VColors.Purple)
        StageRow("Light", 0.50f, "3h 50m", VColors.Blue)
        StageRow("Deep", 0.24f, "1h 55m", VColors.Cyan)
      }
    }
    Text(text = "3 wake events · demo data", color = VColors.InkLo, style = MaterialTheme.typography.bodySmall)
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
  }
}

@Composable
private fun StageRow(label: String, pct: Float, duration: String, color: Color) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(99.dp)).background(color))
    Text(text = label, color = VColors.Ink, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
    Box(
        modifier =
            Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(Color(0x14FFFFFF)),
    ) {
      Box(modifier = Modifier.fillMaxWidth(pct).height(6.dp).background(color))
    }
    Text(text = duration, color = VColors.InkMd, style = MaterialTheme.typography.labelMedium)
  }
}

// ----------------- Mood (demo) -----------------------------------------------

@Composable
private fun StaticMoodSymptoms() {
  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    DateNav(dateLabel = "Today", onPrev = {}, onNext = {})
    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("MOOD")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          listOf("😞", "😕", "😐", "🙂", "😄").forEach { e ->
            Box(
                modifier =
                    Modifier.size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (e == "🙂") Color(0x338B5CF6) else VColors.Card2)
                        .border(1.dp, if (e == "🙂") VColors.Purple else VColors.Border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
              Text(text = e, style = MaterialTheme.typography.headlineMedium)
            }
          }
        }
      }
    }
    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("SYMPTOMS")
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf("Bloating", "Fatigue", "Headache", "Cravings", "Stress", "Anxiety").forEach { s ->
            val active = s == "Fatigue"
            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (active) VColors.Purple else Color.Transparent)
                        .border(1.dp, if (active) VColors.Purple else VColors.Border, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
              Text(text = s, color = if (active) Color.White else VColors.InkMd, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
  }
}

// ----------------- Activity (demo) ------------------------------------------

@Composable
private fun StaticActivity() {
  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    DateNav(dateLabel = "Today", onPrev = {}, onNext = {})
    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("STEPS")
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "6,420", color = VColors.Ink, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
          Text(text = "/10,000", color = VColors.InkLo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
        }
        Sparkline(
            data = listOf(0f, 2f, 8f, 15f, 22f, 35f, 50f, 58f, 64f, 64f),
            color = VColors.Green,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        )
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      StatCard(label = "Active kcal", value = "412", color = VColors.Amber, modifier = Modifier.weight(1f))
      StatCard(label = "Workout", value = "0", sub = "no session yet", color = VColors.Purple, modifier = Modifier.weight(1f))
    }
    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("HEART RATE")
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text(text = "68", color = VColors.Red, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
          Text(text = "avg · min 54 · max 122", color = VColors.InkMd, style = MaterialTheme.typography.bodySmall)
        }
      }
    }
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
  }
}

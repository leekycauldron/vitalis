package com.vitalis.ui.insights

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
import com.vitalis.ui.common.SectionLabel
import com.vitalis.ui.common.Sparkline
import com.vitalis.ui.common.VCard
import com.vitalis.ui.common.VOutlineButton
import com.vitalis.ui.common.VPrimaryButton
import com.vitalis.ui.theme.VColors

private enum class InsightTab { Insights, Personalised, ActionPlan, Progress }

@Composable
fun InsightsHubScreen(modifier: Modifier = Modifier) {
  var tab by remember { mutableStateOf(InsightTab.Insights) }

  Column(modifier = modifier.fillMaxSize().background(VColors.Bg).statusBarsPadding()) {
    Text(
        text = "Insights",
        color = VColors.Ink,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 12.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      InsightTab.entries.forEach { t ->
        Box(
            modifier =
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (tab == t) VColors.Purple else Color.Transparent)
                    .border(1.dp, if (tab == t) VColors.Purple else VColors.Border, RoundedCornerShape(999.dp))
                    .clickable { tab = t }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
          Text(
              text = t.name.replace("ActionPlan", "Action plan"),
              color = if (tab == t) Color.White else VColors.InkMd,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Medium,
          )
        }
      }
    }

    Box(modifier = Modifier.weight(1f)) {
      when (tab) {
        InsightTab.Insights -> Insights()
        InsightTab.Personalised -> Personalised()
        InsightTab.ActionPlan -> ActionPlan()
        InsightTab.Progress -> Progress()
      }
    }
  }
}

// ----------------- Insights (demo) ------------------------------------------

@Composable
private fun Insights() {
  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    VCard(accent = VColors.Purple) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("CORRELATION INSIGHT")
        Text(
            text = "Days you log >30g protein at breakfast correlate with a 12% higher AI Health Score.",
            color = VColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
        )
        listOf("Protein at breakfast", "Sleep quality", "Mid-day energy").forEach { line ->
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(99.dp)).background(VColors.Purple))
            Text(text = line, color = VColors.InkMd, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
    VCard {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("RECOMMENDATION")
        Text(
            text = "Try a high-protein breakfast for 5 days and see if your energy ratings improve.",
            color = VColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
        )
        VOutlineButton(text = "View action plan", onClick = {})
      }
    }
  }
}

// ----------------- Personalised (demo) --------------------------------------

@Composable
private fun Personalised() {
  data class Rec(val tile: Color, val title: String, val sub: String, val desc: String)
  val recs =
      listOf(
          Rec(VColors.Teal, "High-protein breakfast", "+25g protein at AM", "Eggs, Greek yogurt, or a protein shake before 10am."),
          Rec(VColors.Amber, "Caffeine before 1pm", "Better sleep onset", "Cut off coffee earlier — CYP1A2 metaboliser sensitivity."),
          Rec(VColors.Purple, "Vitamin D3 2000 IU", "Daily supplement", "Low sun exposure this week. Take with a fat-containing meal."),
          Rec(VColors.Blue, "Low-glycaemic carbs", "Steadier energy", "Swap white rice for quinoa or sweet potato."),
          Rec(VColors.Pink, "Strength training 3-4×", "Aligns with goal", "Compound lifts: squat, hinge, press, pull."),
      )
  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(text = "Based on your profile, we recommend:", color = VColors.InkMd, style = MaterialTheme.typography.bodyMedium)
    recs.forEach { r ->
      VCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(r.tile.copy(alpha = 0.22f)))
          Column(modifier = Modifier.weight(1f)) {
            Text(text = r.title, color = VColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = r.sub, color = VColors.PurpleL, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(text = r.desc, color = VColors.InkMd, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
    VPrimaryButton(text = "View Action Plan", onClick = {})
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
  }
}

// ----------------- ActionPlan (demo) ----------------------------------------

@Composable
private fun ActionPlan() {
  val items = listOf(
      "Eat 30g protein at breakfast" to true,
      "Stay under 250g carbs today" to false,
      "Get to bed by 10:30pm" to false,
      "Take Vitamin D3 with lunch" to true,
      "Walk 8,000 steps" to false,
  )
  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf("Today", "This week", "This month").forEachIndexed { idx, label ->
        Box(
            modifier =
                Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (idx == 0) VColors.Purple else Color.Transparent)
                    .border(1.dp, if (idx == 0) VColors.Purple else VColors.Border, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
          Text(text = label, color = if (idx == 0) Color.White else VColors.InkMd, style = MaterialTheme.typography.labelMedium)
        }
      }
    }
    items.forEach { (action, done) ->
      VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Box(
              modifier =
                  Modifier.size(22.dp).clip(RoundedCornerShape(8.dp))
                      .background(if (done) VColors.Teal else Color.Transparent)
                      .border(1.5.dp, if (done) VColors.Teal else VColors.InkLo, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center,
          ) {
            if (done) Text(text = "✓", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
          }
          Text(
              text = action,
              color = if (done) VColors.InkLo else VColors.Ink,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
              textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
          )
          if (done) Text(text = "✓", color = VColors.Teal, style = MaterialTheme.typography.titleLarge)
        }
      }
    }
    VCard(background = Color(0x3310B981)) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "${items.count { it.second }} of ${items.size} completed", color = VColors.Teal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(text = "Keep stacking wins.", color = VColors.Teal.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
      }
    }
    VPrimaryButton(text = "Mark all completed", onClick = {})
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
  }
}

// ----------------- Progress (demo) ------------------------------------------

@Composable
private fun Progress() {
  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    ProgressCard(label = "Health Score", value = "72", change = "+6", color = VColors.Purple, data = listOf(60f, 62f, 65f, 64f, 67f, 70f, 72f))
    ProgressCard(label = "Energy Trend", value = "+14%", change = "↑", color = VColors.Teal, data = listOf(40f, 45f, 50f, 48f, 52f, 56f, 58f))
    ProgressCard(label = "Consistency", value = "87%", change = "Great job!", color = VColors.Cyan, data = listOf(70f, 72f, 78f, 80f, 84f, 86f, 87f))
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))
  }
}

@Composable
private fun ProgressCard(label: String, value: String, change: String, color: Color, data: List<Float>) {
  VCard {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      SectionLabel(label)
      Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(text = value, color = VColors.Ink, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Sparkline(data = data, color = color, modifier = Modifier.height(40.dp).fillMaxWidth(0.55f))
      }
      Text(text = change, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
  }
}

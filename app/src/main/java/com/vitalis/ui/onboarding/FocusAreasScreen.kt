package com.vitalis.ui.onboarding

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.vitalis.profile.FocusArea
import com.vitalis.ui.common.ScreenHeader
import com.vitalis.ui.common.Title
import com.vitalis.ui.common.VPrimaryButton
import com.vitalis.ui.theme.VColors

@Composable
fun FocusAreasScreen(
    initial: Set<FocusArea>,
    onBack: () -> Unit,
    onContinue: (Set<FocusArea>) -> Unit,
    modifier: Modifier = Modifier,
) {
  var selected by remember { mutableStateOf(initial) }

  Column(modifier = modifier.fillMaxSize().background(VColors.Bg).navigationBarsPadding()) {
    ScreenHeader(step = 1, total = 3, onBack = onBack)
    Title(text = "What do you want to focus on?", subtitle = "Select all that apply.")

    val rows = FocusArea.entries.chunked(2)
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      rows.forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          row.forEach { fa ->
            FocusCard(
                area = fa,
                selected = fa in selected,
                onToggle = {
                  selected = if (fa in selected) selected - fa else selected + fa
                },
                modifier = Modifier.weight(1f),
            )
          }
          if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
      }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)) {
      VPrimaryButton(text = "Continue", onClick = { onContinue(selected) })
    }
  }
}

@Composable
private fun FocusCard(area: FocusArea, selected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
  val tint = Color(area.colorHex)
  Box(
      modifier =
          modifier
              .height(120.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(tint.copy(alpha = if (selected) 0.18f else 0.10f))
              .border(1.dp, if (selected) tint else VColors.Border, RoundedCornerShape(14.dp))
              .clickable(onClick = onToggle)
              .padding(14.dp),
  ) {
    Column {
      Box(
          modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.25f)),
          contentAlignment = Alignment.Center,
      ) {
        Text(text = areaEmoji(area), style = MaterialTheme.typography.titleLarge)
      }
      Spacer(modifier = Modifier.weight(1f))
      Text(
          text = area.label,
          color = VColors.Ink,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
      )
    }
    if (selected) {
      Box(
          modifier =
              Modifier.size(22.dp)
                  .align(Alignment.TopEnd)
                  .clip(RoundedCornerShape(99.dp))
                  .background(VColors.Purple),
          contentAlignment = Alignment.Center,
      ) {
        Text(text = "✓", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
      }
    }
  }
}

private fun areaEmoji(a: FocusArea): String =
    when (a) {
      FocusArea.ENERGY -> "⚡"
      FocusArea.SLEEP -> "🌙"
      FocusArea.NUTRITION -> "🥗"
      FocusArea.FITNESS -> "💪"
      FocusArea.MOOD -> "🧘"
      FocusArea.LONGEVITY -> "🌱"
    }

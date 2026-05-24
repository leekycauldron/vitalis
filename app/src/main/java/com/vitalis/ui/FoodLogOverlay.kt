package com.vitalis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitalis.foodlog.db.FoodLogEntity

/**
 * Slim card pinned above the bottom-bar buttons. Renders the most recent food log entries while
 * the assistant is in LOGGING phase. Scrollable; capped at [MAX_HEIGHT] so it never devours the
 * stream preview.
 */
private val MAX_HEIGHT = 240.dp

@Composable
fun FoodLogOverlay(entries: List<FoodLogEntity>, modifier: Modifier = Modifier) {
  if (entries.isEmpty()) return

  val listState = rememberLazyListState()

  // Newest entries arrive at index 0 (Room query is ORDER BY ts DESC). Auto-scroll to top so the
  // user always sees the most recent without manual intervention.
  LaunchedEffect(entries.firstOrNull()?.id) {
    if (entries.isNotEmpty()) listState.animateScrollToItem(0)
  }

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .heightIn(max = MAX_HEIGHT)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xCC000000)),
  ) {
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
          text = "Food log",
          color = Color.White,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      Text(
          text = "${entries.size} item${if (entries.size == 1) "" else "s"} · ${totalCalories(entries)} kcal",
          color = Color.White.copy(alpha = 0.7f),
          style = MaterialTheme.typography.labelSmall,
      )
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(entries, key = { it.id }) { entry -> FoodLogRow(entry) }
    }
  }
}

@Composable
private fun FoodLogRow(entry: FoodLogEntity) {
  Row(
      modifier = Modifier.fillMaxWidth().height(22.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (entry.isJunk) {
      Text(text = "⚠", color = Color(0xFFFF8A8A), style = MaterialTheme.typography.bodySmall)
    }
    Text(
        text = entry.name,
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.weight(1f),
    )
    Text(
        text = macrosLine(entry),
        color = Color.White.copy(alpha = 0.75f),
        style = MaterialTheme.typography.labelSmall,
    )
  }
}

private fun totalCalories(entries: List<FoodLogEntity>): Int = entries.sumOf { it.calories }

private fun macrosLine(e: FoodLogEntity): String {
  val cal = e.calories
  val p = formatGrams(e.proteinG)
  val c = formatGrams(e.carbsG)
  val f = formatGrams(e.fatG)
  return "$cal kcal · ${p}P ${c}C ${f}F"
}

private fun formatGrams(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

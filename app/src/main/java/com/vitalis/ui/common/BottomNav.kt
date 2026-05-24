package com.vitalis.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.vitalis.ui.theme.VColors

enum class VTab { Home, Log, Add, Assistant, Profile }

@Composable
fun BottomNav(
    active: VTab,
    onSelect: (VTab) -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .background(Color(0xD9080C1A))
              .border(1.dp, VColors.Border, RoundedCornerShape(0.dp))
              .navigationBarsPadding()
              .padding(top = 8.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceAround,
  ) {
    NavTabItem(label = "Home", active = active == VTab.Home, onClick = { onSelect(VTab.Home) }) { c -> HomeIcon(c) }
    NavTabItem(label = "Log", active = active == VTab.Log, onClick = { onSelect(VTab.Log) }) { c -> LogIcon(c) }
    AddButton(onClick = { onSelect(VTab.Add) })
    NavTabItem(label = "Assistant", active = active == VTab.Assistant, onClick = { onSelect(VTab.Assistant) }) { c -> AssistantIcon(c) }
    NavTabItem(label = "Profile", active = active == VTab.Profile, onClick = { onSelect(VTab.Profile) }) { c -> ProfileIcon(c) }
  }
}

@Composable
private fun NavTabItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
  val tint = if (active) VColors.PurpleL else VColors.InkLo
  Column(
      modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    icon(tint)
    Text(
        text = label,
        color = tint,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
    )
  }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
  Box(
      modifier =
          Modifier.size(52.dp)
              .offset(y = (-14).dp)
              .clip(RoundedCornerShape(16.dp))
              .background(
                  Brush.linearGradient(listOf(VColors.PurpleL, VColors.Purple))
              )
              .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(22.dp)) {
      val s = 2.4.dp.toPx()
      drawLine(Color.White, Offset(size.width / 2f, size.height * 0.15f), Offset(size.width / 2f, size.height * 0.85f), s, cap = StrokeCap.Round)
      drawLine(Color.White, Offset(size.width * 0.15f, size.height / 2f), Offset(size.width * 0.85f, size.height / 2f), s, cap = StrokeCap.Round)
    }
  }
}

// ----- Icons -----------------------------------------------------------------

@Composable
fun HomeIcon(color: Color = VColors.InkLo) {
  Canvas(modifier = Modifier.size(22.dp)) {
    val path = Path().apply {
      moveTo(size.width * 0.14f, size.height * 0.5f)
      lineTo(size.width * 0.5f, size.height * 0.18f)
      lineTo(size.width * 0.86f, size.height * 0.5f)
      lineTo(size.width * 0.86f, size.height * 0.86f)
      lineTo(size.width * 0.6f, size.height * 0.86f)
      lineTo(size.width * 0.6f, size.height * 0.58f)
      lineTo(size.width * 0.4f, size.height * 0.58f)
      lineTo(size.width * 0.4f, size.height * 0.86f)
      lineTo(size.width * 0.14f, size.height * 0.86f)
      close()
    }
    drawPath(path, color, style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round))
  }
}

@Composable
fun LogIcon(color: Color = VColors.InkLo) {
  Canvas(modifier = Modifier.size(22.dp)) {
    val s = 1.6.dp.toPx()
    val w = size.width
    val h = size.height
    val page = Path().apply {
      moveTo(w * 0.20f, h * 0.14f)
      lineTo(w * 0.80f, h * 0.14f)
      lineTo(w * 0.80f, h * 0.86f)
      lineTo(w * 0.20f, h * 0.86f)
      close()
    }
    drawPath(page, color, style = Stroke(s, join = StrokeJoin.Round, cap = StrokeCap.Round))
    drawLine(color, Offset(w * 0.30f, h * 0.36f), Offset(w * 0.70f, h * 0.36f), s, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.30f, h * 0.52f), Offset(w * 0.70f, h * 0.52f), s, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.30f, h * 0.68f), Offset(w * 0.58f, h * 0.68f), s, cap = StrokeCap.Round)
  }
}

@Composable
fun AssistantIcon(color: Color = VColors.InkLo) {
  Canvas(modifier = Modifier.size(22.dp)) {
    val s = 1.6.dp.toPx()
    val w = size.width
    val h = size.height
    val r = w * 0.20f
    drawCircle(color, radius = r, center = Offset(w * 0.28f, h * 0.55f), style = Stroke(s))
    drawCircle(color, radius = r, center = Offset(w * 0.72f, h * 0.55f), style = Stroke(s))
    drawLine(color, Offset(w * 0.48f, h * 0.55f), Offset(w * 0.52f, h * 0.55f), s, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.08f, h * 0.45f), Offset(w * 0.16f, h * 0.40f), s, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.84f, h * 0.40f), Offset(w * 0.92f, h * 0.45f), s, cap = StrokeCap.Round)
  }
}

@Composable
fun ProfileIcon(color: Color = VColors.InkLo) {
  Canvas(modifier = Modifier.size(22.dp)) {
    val s = 1.6.dp.toPx()
    val w = size.width
    val h = size.height
    drawCircle(color, radius = h * 0.18f, center = Offset(w / 2f, h * 0.36f), style = Stroke(s))
    val arc = Path().apply {
      moveTo(w * 0.18f, h * 0.86f)
      quadraticBezierTo(w * 0.5f, h * 0.55f, w * 0.82f, h * 0.86f)
    }
    drawPath(arc, color, style = Stroke(s, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

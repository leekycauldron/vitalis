package com.vitalis.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalis.ui.theme.VColors

// ----- Card ------------------------------------------------------------------

/** Standard Vitalis card: 14dp radius, translucent border, optional left accent stripe. */
@Composable
fun VCard(
    modifier: Modifier = Modifier,
    background: Color = VColors.Card,
    accent: Color? = null,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
  val shape = RoundedCornerShape(14.dp)
  val base =
      modifier
          .clip(shape)
          .background(background)
          .border(1.dp, VColors.Border, shape)
          .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
  Box(modifier = base) {
    if (accent != null) {
      Box(
          modifier =
              Modifier.fillMaxWidth(0f)
                  .width(3.dp)
                  .background(accent)
                  .align(Alignment.CenterStart)
      )
    }
    Box(modifier = Modifier.padding(padding).then(if (accent != null) Modifier.padding(start = 3.dp) else Modifier)) {
      content()
    }
  }
}

// ----- Chip ------------------------------------------------------------------

@Composable
fun VChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = VColors.Purple,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
  val bg = if (active) color else Color.Transparent
  val border = if (active) color else Color(0x1AFFFFFF)
  val textColor = if (active) Color.White else VColors.InkMd
  Box(
      modifier =
          modifier
              .clip(RoundedCornerShape(999.dp))
              .background(bg)
              .border(1.dp, border, RoundedCornerShape(999.dp))
              .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
              .padding(horizontal = 14.dp, vertical = 7.dp),
  ) {
    Text(text = text, color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
  }
}

// ----- Buttons ---------------------------------------------------------------

@Composable
fun VPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = VColors.Purple,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
  val effectiveColor = if (enabled) color else Color(0xFF333A52)
  Row(
      modifier =
          modifier
              .height(52.dp)
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(effectiveColor)
              .clickable(enabled = enabled, onClick = onClick),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
  ) {
    leading?.invoke()
    if (leading != null) Spacer(modifier = Modifier.width(8.dp))
    Text(
        text = text,
        color = if (enabled) Color.White else VColors.InkLo,
        style = MaterialTheme.typography.titleMedium,
    )
  }
}

@Composable
fun VOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = VColors.Ink,
    enabled: Boolean = true,
) {
  Row(
      modifier =
          modifier
              .height(52.dp)
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(14.dp))
              .clickable(enabled = enabled, onClick = onClick),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
  ) {
    Text(text = text, color = color, style = MaterialTheme.typography.titleMedium)
  }
}

// ----- ProgressRing ----------------------------------------------------------

/** Circular SVG-equivalent ring with a value and label below. */
@Composable
fun ProgressRing(
    value: Int,
    max: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 70.dp,
    stroke: Dp = 6.dp,
    unit: String? = null,
) {
  val pct = if (max > 0) (value.toFloat() / max).coerceAtMost(1f) else 0f
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
      Canvas(modifier = Modifier.size(size)) {
        val strokePx = stroke.toPx()
        val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
        val topLeft = Offset(strokePx / 2f, strokePx / 2f)
        drawArc(
            color = Color(0x10FFFFFF),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * pct,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            color = VColors.Ink,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, letterSpacing = (-0.3).sp),
        )
        if (unit != null) {
          Text(text = unit, color = VColors.InkLo, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
        }
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = label, color = VColors.Ink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    Text(text = "/${max}", color = VColors.InkLo, style = MaterialTheme.typography.labelSmall)
  }
}

// ----- Sparkline -------------------------------------------------------------

@Composable
fun Sparkline(data: List<Float>, color: Color, modifier: Modifier = Modifier, filled: Boolean = true) {
  if (data.size < 2) {
    Spacer(modifier = modifier)
    return
  }
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val min = data.min()
    val max = data.max()
    val range = (max - min).takeIf { it > 0f } ?: 1f
    val pts =
        data.mapIndexed { i, v ->
          val x = (i.toFloat() / (data.size - 1).toFloat()) * w
          val y = h - ((v - min) / range) * (h - 4.dp.toPx()) - 2.dp.toPx()
          Offset(x, y)
        }
    val line = Path().apply {
      moveTo(pts[0].x, pts[0].y)
      for (p in pts.drop(1)) lineTo(p.x, p.y)
    }
    if (filled) {
      val fill = Path().apply {
        addPath(line)
        lineTo(w, h)
        lineTo(0f, h)
        close()
      }
      drawPath(
          fill,
          brush = Brush.verticalGradient(0f to color.copy(alpha = 0.4f), 1f to Color.Transparent),
      )
    }
    drawPath(line, color = color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
  }
}

// ----- MacroBar --------------------------------------------------------------

@Composable
fun MacroBar(label: String, value: Double, target: Double, color: Color, modifier: Modifier = Modifier, unit: String = "g") {
  val pct = if (target > 0) (value / target).coerceAtMost(1.0).toFloat() else 0f
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = label, color = VColors.Ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
      Text(
          text = "${formatNum(value)}/${formatNum(target)}$unit",
          color = VColors.InkMd,
          style = MaterialTheme.typography.labelMedium,
      )
    }
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x14FFFFFF)),
    ) {
      Box(
          modifier =
              Modifier.fillMaxWidth(pct).height(6.dp).clip(RoundedCornerShape(999.dp)).background(color)
      )
    }
  }
}

private fun formatNum(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else "%.0f".format(v)

// ----- StatCard --------------------------------------------------------------

@Composable
fun StatCard(
    label: String,
    value: String,
    sub: String? = null,
    color: Color = VColors.Ink,
    modifier: Modifier = Modifier,
) {
  VCard(modifier = modifier, padding = 14.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(text = label.uppercase(), color = VColors.InkLo, style = MaterialTheme.typography.labelSmall)
      Text(
          text = value,
          color = color,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
      )
      if (sub != null) {
        Text(text = sub, color = VColors.InkMd, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

// ----- DashedDivider ---------------------------------------------------------

@Composable
fun DashedDivider(modifier: Modifier = Modifier, color: Color = VColors.Border) {
  Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
    val pe = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()), 0f)
    drawIntoCanvasDashed(color, pe)
  }
}

private fun DrawScope.drawIntoCanvasDashed(color: Color, effect: PathEffect) {
  drawLine(
      color = color,
      start = Offset(0f, size.height / 2f),
      end = Offset(size.width, size.height / 2f),
      strokeWidth = 1.dp.toPx(),
      pathEffect = effect,
  )
}

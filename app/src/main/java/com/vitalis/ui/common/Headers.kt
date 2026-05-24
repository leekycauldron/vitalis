package com.vitalis.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalis.R
import com.vitalis.ui.theme.VColors

// ----- VitalisLogo + Wordmark ------------------------------------------------

@Composable
fun VitalisLogo(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 32.dp) {
  Image(
      painter = painterResource(id = R.drawable.inapp_icon),
      contentDescription = null,
      modifier = modifier.size(size),
  )
}

@Composable
fun Wordmark(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.TextUnit = 20.sp, color: Color = VColors.Ink) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    VitalisLogo(size = 28.dp)
    Text(
        text = "Vitalis",
        color = color,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = size, letterSpacing = (-0.6).sp),
    )
  }
}

// ----- AppHeader -------------------------------------------------------------

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    centerTitle: Boolean = false,
) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (onBack != null) {
      Box(
          modifier =
              Modifier.size(38.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0x0AFFFFFF))
                  .border(1.dp, VColors.Border, RoundedCornerShape(12.dp))
                  .clickable(onClick = onBack),
          contentAlignment = Alignment.Center,
      ) {
        BackChevron()
      }
    }
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = if (centerTitle) Alignment.CenterHorizontally else Alignment.Start,
    ) {
      if (title != null) {
        Text(
            text = title,
            color = VColors.Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
      }
      if (subtitle != null) {
        Text(text = subtitle, color = VColors.InkMd, style = MaterialTheme.typography.bodySmall)
      }
    }
    trailing?.invoke()
  }
}

// ----- BackChevron + Arrow ---------------------------------------------------

@Composable
fun BackChevron(color: Color = VColors.InkMd) {
  Canvas(modifier = Modifier.size(10.dp, 16.dp)) {
    val stroke = 2.2.dp.toPx()
    drawLine(
        color = color,
        start = Offset(size.width * 0.8f, 0f),
        end = Offset(size.width * 0.2f, size.height / 2f),
        strokeWidth = stroke,
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.2f, size.height / 2f),
        end = Offset(size.width * 0.8f, size.height),
        strokeWidth = stroke,
    )
  }
}

// ----- StepBar ---------------------------------------------------------------

@Composable
fun StepBar(step: Int, total: Int, modifier: Modifier = Modifier) {
  Row(
      modifier = modifier.fillMaxWidth().padding(horizontal = 22.dp),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    for (i in 0 until total) {
      val on = i <= step
      Box(
          modifier =
              Modifier.weight(1f)
                  .height(3.dp)
                  .clip(RoundedCornerShape(3.dp))
                  .background(if (on) VColors.Purple else Color(0x14FFFFFF))
      )
    }
  }
}

// ----- ScreenHeader (onboarding) ---------------------------------------------

@Composable
fun ScreenHeader(
    step: Int,
    total: Int = 3,
    hideBack: Boolean = false,
    label: String? = null,
    onBack: (() -> Unit)? = null,
) {
  Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
    Row(
        modifier =
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 22.dp).padding(bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      if (!hideBack) {
        Box(
            modifier =
                Modifier.size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x0AFFFFFF))
                    .border(1.dp, VColors.Border, RoundedCornerShape(12.dp))
                    .clickable(enabled = onBack != null, onClick = { onBack?.invoke() }),
            contentAlignment = Alignment.Center,
        ) {
          BackChevron()
        }
      } else {
        Spacer(modifier = Modifier.size(38.dp))
      }
      if (label != null) {
        Text(
            text = label.uppercase(),
            color = VColors.InkLo,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
            fontWeight = FontWeight.Medium,
        )
      }
      Text(
          text = "${step + 1}/$total",
          color = VColors.InkLo,
          style = MaterialTheme.typography.labelMedium,
      )
    }
    Spacer(modifier = Modifier.height(14.dp))
    StepBar(step = step, total = total)
  }
}

// ----- Title -----------------------------------------------------------------

@Composable
fun Title(text: String, modifier: Modifier = Modifier, subtitle: String? = null) {
  Column(modifier = modifier.padding(horizontal = 22.dp, vertical = 24.dp)) {
    Text(
        text = text,
        color = VColors.Ink,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.SemiBold,
    )
    if (subtitle != null) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(text = subtitle, color = VColors.InkMd, style = MaterialTheme.typography.bodyLarge)
    }
  }
}

// ----- DateNav ---------------------------------------------------------------

@Composable
fun DateNav(
    dateLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    topLabel: String = "Today",
) {
  Row(
      modifier =
          modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    NavArrow(direction = "left", onClick = onPrev)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
          text = topLabel.uppercase(),
          color = VColors.InkLo,
          style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
      )
      Text(text = dateLabel, color = VColors.Ink, style = MaterialTheme.typography.titleMedium)
    }
    NavArrow(direction = "right", onClick = onNext)
  }
}

@Composable
private fun NavArrow(direction: String, onClick: () -> Unit) {
  Box(
      modifier =
          Modifier.size(32.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0x0AFFFFFF))
              .border(1.dp, VColors.Border, RoundedCornerShape(10.dp))
              .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(8.dp, 12.dp)) {
      val s = 1.8.dp.toPx()
      val rightFlip = direction == "right"
      val xStart = if (rightFlip) size.width * 0.25f else size.width * 0.75f
      val xEnd = if (rightFlip) size.width * 0.75f else size.width * 0.25f
      drawLine(VColors.InkMd, Offset(xStart, 0f), Offset(xEnd, size.height / 2f), s)
      drawLine(VColors.InkMd, Offset(xEnd, size.height / 2f), Offset(xStart, size.height), s)
    }
  }
}

// ----- SectionLabel ----------------------------------------------------------

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
  Text(
      text = text.uppercase(),
      modifier = modifier,
      color = VColors.InkLo,
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
      fontWeight = FontWeight.Medium,
  )
}

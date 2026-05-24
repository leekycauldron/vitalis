package com.vitalis.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.vitalis.ui.theme.VColors

@Composable
fun PulsatingDot(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = VColors.PurpleL,
) {
  val transition = rememberInfiniteTransition(label = "pulse")
  val scale by
      transition.animateFloat(
          initialValue = 0.85f,
          targetValue = 1.15f,
          animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
          label = "scale",
      )
  val alpha by
      transition.animateFloat(
          initialValue = 0.55f,
          targetValue = 0.95f,
          animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
          label = "alpha",
      )
  Box(
      modifier =
          modifier
              .scale(scale)
              .clip(CircleShape)
              .background(color.copy(alpha = alpha))
              .clickable(onClick = onClick),
  )
}

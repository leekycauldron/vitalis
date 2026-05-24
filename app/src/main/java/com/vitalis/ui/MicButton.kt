package com.vitalis.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MicButton(
    listening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "mic")
  val scale by
      transition.animateFloat(
          initialValue = 1f,
          targetValue = if (listening) 1.15f else 1f,
          animationSpec =
              infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
          label = "mic-scale",
      )

  Box(
      modifier =
          modifier
              .size(72.dp)
              .scale(scale)
              .clip(CircleShape)
              .background(
                  if (listening) Color(0xFFFF6B6B) else Color(0xFF3D6AFF)
              )
              .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = "Voice assistant",
        tint = Color.White,
        modifier = Modifier.size(32.dp),
    )
  }
}

package com.vitalis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitalis.voice.VoicePhase
import com.vitalis.voice.VoiceState

@Composable
fun VoiceOverlay(
    state: VoiceState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
  if (state.phase == VoicePhase.IDLE && state.response.isNullOrBlank() && state.transcript.isBlank()) {
    return
  }
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xEE0A0A0A))
              .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      PhaseLabel(state.phase)
      TextButton(onClick = onDismiss) {
        Text("Dismiss", color = Color.White.copy(alpha = 0.7f))
      }
    }

    if (state.transcript.isNotBlank()) {
      Text(
          text = "You: ${state.transcript}",
          color = Color.White.copy(alpha = 0.9f),
          style = MaterialTheme.typography.bodyMedium,
          fontStyle = if (state.phase == VoicePhase.LISTENING) FontStyle.Italic else FontStyle.Normal,
      )
    } else if (state.phase == VoicePhase.LISTENING) {
      Text(
          text = "Listening…",
          color = Color.White.copy(alpha = 0.7f),
          style = MaterialTheme.typography.bodyMedium,
          fontStyle = FontStyle.Italic,
      )
    }

    when (state.phase) {
      VoicePhase.THINKING -> {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              strokeWidth = 2.dp,
              color = Color.White.copy(alpha = 0.7f),
          )
          Text(
              text = "  Thinking…",
              color = Color.White.copy(alpha = 0.7f),
              style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
      VoicePhase.SPEAKING -> {
        state.response?.let { ResponseText(it) }
      }
      VoicePhase.ERROR -> {
        state.errorMessage?.let { msg ->
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .background(Color(0xCC8B0000))
                      .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Text(
                text = msg,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }
      VoicePhase.IDLE -> {
        state.response?.let { ResponseText(it) }
      }
      VoicePhase.LISTENING -> {} // handled above
    }
  }
}

@Composable
private fun PhaseLabel(phase: VoicePhase) {
  val (text, color) =
      when (phase) {
        VoicePhase.LISTENING -> "LISTENING" to Color(0xFFFF6B6B)
        VoicePhase.THINKING -> "THINKING" to Color(0xFFFFC857)
        VoicePhase.SPEAKING -> "REPLYING" to Color(0xFF7DD3A0)
        VoicePhase.ERROR -> "ERROR" to Color(0xFFFF8A8A)
        VoicePhase.IDLE -> "DONE" to Color.White.copy(alpha = 0.6f)
      }
  Text(
      text = text,
      color = color,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
  )
}

@Composable
private fun ResponseText(text: String) {
  Text(
      text = "Vitalis: $text",
      color = Color.White,
      style = MaterialTheme.typography.bodyMedium,
  )
}

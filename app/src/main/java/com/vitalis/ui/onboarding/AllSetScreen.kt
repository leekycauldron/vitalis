package com.vitalis.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitalis.ui.common.ScreenHeader
import com.vitalis.ui.common.Sparkline
import com.vitalis.ui.common.Title
import com.vitalis.ui.common.VCard
import com.vitalis.ui.common.VPrimaryButton
import com.vitalis.ui.theme.VColors

@Composable
fun AllSetScreen(
    nameOrEmail: String,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().background(VColors.Bg).navigationBarsPadding(),
  ) {
    ScreenHeader(step = 2, total = 3, hideBack = true)
    Title(
        text = "You're all set${if (nameOrEmail.isNotBlank()) ", ${nameOrEmail.substringBefore('@')}" else ""}.",
        subtitle = "Here's your baseline. Vitalis will personalise it as you go.",
    )

    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier.size(120.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(VColors.PurpleL, VColors.Purple, VColors.PurpleD)
                        )
                    ),
            contentAlignment = Alignment.Center,
        ) {
          Text(text = "✓", color = Color.White, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
        }
      }

      VCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(text = "AI HEALTH SCORE", color = VColors.InkLo, style = MaterialTheme.typography.labelSmall)
          Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = "72", color = VColors.Ink, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Sparkline(
                data = listOf(60f, 62f, 61f, 65f, 67f, 70f, 72f),
                color = VColors.Purple,
                modifier = Modifier.height(40.dp).fillMaxWidth().padding(start = 16.dp),
            )
          }
          Text(text = "Baseline · we'll start tracking as you log meals.", color = VColors.InkMd, style = MaterialTheme.typography.bodySmall)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NextStep(text = "Connect your Ray-Ban Meta glasses for passive food tracking.")
        NextStep(text = "Add your first meal — manually or with voice.")
        NextStep(text = "Drop in any DNA notes to deepen the recommendations.")
      }

      Spacer(modifier = Modifier.height(8.dp))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)) {
      VPrimaryButton(text = "Get Started", onClick = onGetStarted)
    }
  }
}

@Composable
private fun NextStep(text: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(99.dp)).background(VColors.Purple))
    Text(text = text, color = VColors.Ink, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Text(text = "›", color = VColors.InkLo, style = MaterialTheme.typography.titleLarge)
  }
}

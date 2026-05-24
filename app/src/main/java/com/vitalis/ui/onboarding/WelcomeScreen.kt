package com.vitalis.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalis.ui.common.VOutlineButton
import com.vitalis.ui.common.VPrimaryButton
import com.vitalis.ui.common.Wordmark
import com.vitalis.ui.theme.VColors

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().background(VColors.Bg)) {
    // ambient glows
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
          brush = Brush.radialGradient(listOf(Color(0x558B5CF6), Color.Transparent), center = Offset(size.width * 0.3f, size.height * 0.35f), radius = size.width * 0.85f),
          radius = size.width * 0.85f,
          center = Offset(size.width * 0.3f, size.height * 0.35f),
      )
      drawCircle(
          brush = Brush.radialGradient(listOf(Color(0x333B82F6), Color.Transparent), center = Offset(size.width * 0.9f, size.height * 0.55f), radius = size.width * 0.5f),
          radius = size.width * 0.5f,
          center = Offset(size.width * 0.9f, size.height * 0.55f),
      )
      // orbit rings
      drawCircle(
          color = Color(0x0DFFFFFF),
          radius = size.width * 0.85f,
          center = Offset(size.width / 2f, size.height * 0.42f),
          style = Stroke(width = 1f),
      )
      drawCircle(
          color = Color(0x12FFFFFF),
          radius = size.width * 0.62f,
          center = Offset(size.width / 2f, size.height * 0.42f),
          style = Stroke(width = 1f),
      )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.height(120.dp))
      Wordmark(size = 22.sp)
      Spacer(modifier = Modifier.height(80.dp))

      // Glowing orb
      Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawCircle(
              brush = Brush.radialGradient(
                  colors = listOf(
                      Color(0xFFDDD6FE),
                      VColors.PurpleL,
                      VColors.Purple,
                      VColors.PurpleD,
                      Color(0xFF1E1147),
                  ),
                  center = Offset(size.width * 0.38f, size.height * 0.38f),
                  radius = size.width * 0.7f,
              ),
              radius = size.width / 2f,
              center = Offset(size.width / 2f, size.height / 2f),
          )
        }
        Box(modifier = Modifier.size(48.dp).align(Alignment.TopStart).padding(start = 36.dp, top = 28.dp)
            .clip(CircleShape).background(Color(0x66FFFFFF)))
      }

      Spacer(modifier = Modifier.height(80.dp))
      Text(
          text = "Your Health AI",
          color = VColors.Ink,
          fontWeight = FontWeight.SemiBold,
          style = MaterialTheme.typography.headlineLarge,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
          text = "Personalised to You.",
          color = VColors.InkMd,
          style = MaterialTheme.typography.bodyLarge,
      )

      Spacer(modifier = Modifier.weight(1f))

      VPrimaryButton(text = "Create Account", onClick = onCreateAccount)
      Spacer(modifier = Modifier.height(12.dp))
      VOutlineButton(text = "Log In", onClick = onLogIn)
      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}

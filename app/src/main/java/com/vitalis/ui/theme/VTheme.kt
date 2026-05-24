package com.vitalis.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Vitalis color palette (mirrors `app-tokens.jsx` from the design handoff). */
object VColors {
  val Bg = Color(0xFF080C1A)
  val Card = Color(0xFF10162A)
  val Card2 = Color(0xFF161D35)

  val Border = Color(0x0FFFFFFF) // 6% white
  val BorderPurple = Color(0x338B5CF6) // 20% purple

  val Purple = Color(0xFF8B5CF6)
  val PurpleL = Color(0xFFA78BFA)
  val PurpleD = Color(0xFF6D28D9)
  val Teal = Color(0xFF10B981)
  val Amber = Color(0xFFF59E0B)
  val Red = Color(0xFFEF4444)
  val Blue = Color(0xFF3B82F6)
  val Cyan = Color(0xFF06B6D4)
  val Pink = Color(0xFFEC4899)
  val Green = Color(0xFF22C55E)

  val Ink = Color(0xFFF5F5F7)
  val InkMd = Color(0xFFA3ACC2)
  val InkLo = Color(0xFF6B7390)

  /** Generic translucent surfaces. */
  val Glass = Color(0xD90A0A0A)
  val ScrimLight = Color(0x14FFFFFF)
}

val VShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

/** Compose's `FontFamily.Default` resolves to system fonts (SF / Roboto). Keep that. */
val VTypography: Typography = run {
  val title = TextStyle(letterSpacing = (-0.4).sp, fontWeight = FontWeight.SemiBold)
  val body = TextStyle(letterSpacing = (-0.1).sp)
  Typography(
      displayLarge = title.copy(fontSize = 36.sp, lineHeight = 40.sp),
      displayMedium = title.copy(fontSize = 30.sp, lineHeight = 36.sp),
      displaySmall = title.copy(fontSize = 26.sp, lineHeight = 32.sp),
      headlineLarge = title.copy(fontSize = 28.sp, lineHeight = 34.sp),
      headlineMedium = title.copy(fontSize = 22.sp, lineHeight = 28.sp),
      headlineSmall = title.copy(fontSize = 19.sp, lineHeight = 24.sp),
      titleLarge = title.copy(fontSize = 17.sp, lineHeight = 22.sp),
      titleMedium = title.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
      titleSmall = title.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
      bodyLarge = body.copy(fontSize = 16.sp, lineHeight = 22.sp),
      bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
      bodySmall = body.copy(fontSize = 12.sp, lineHeight = 16.sp),
      labelLarge = body.copy(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
      labelMedium = body.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
      labelSmall = body.copy(fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
  )
}

private val VColorScheme =
    darkColorScheme(
        primary = VColors.Purple,
        onPrimary = Color.White,
        primaryContainer = VColors.PurpleD,
        onPrimaryContainer = Color.White,
        secondary = VColors.Teal,
        onSecondary = Color.White,
        tertiary = VColors.Amber,
        background = VColors.Bg,
        onBackground = VColors.Ink,
        surface = VColors.Card,
        onSurface = VColors.Ink,
        surfaceVariant = VColors.Card2,
        onSurfaceVariant = VColors.InkMd,
        error = VColors.Red,
        onError = Color.White,
        outline = VColors.Border,
    )

/** Whether the system has a system-bar tint we should respect. Toggled in Activity. */
val LocalIsSystemBarsLight = staticCompositionLocalOf { false }

@Composable
fun VTheme(content: @Composable () -> Unit) {
  MaterialTheme(
      colorScheme = VColorScheme,
      typography = VTypography,
      shapes = VShapes,
      content = content,
  )
}

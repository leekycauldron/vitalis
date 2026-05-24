package com.vitalis.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vitalis.profile.ActivityLevel
import com.vitalis.profile.Gender
import com.vitalis.profile.PrimaryGoal
import com.vitalis.profile.UserProfile
import com.vitalis.ui.common.ScreenHeader
import com.vitalis.ui.common.SectionLabel
import com.vitalis.ui.common.Title
import com.vitalis.ui.common.VPrimaryButton
import com.vitalis.ui.theme.VColors

@Composable
fun AboutYouScreen(
    initial: UserProfile,
    onBack: () -> Unit,
    onContinue: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
  var age by remember { mutableStateOf(initial.age ?: 30) }
  var gender by remember { mutableStateOf(initial.gender) }
  var heightCm by remember { mutableStateOf(initial.heightCm?.toString() ?: "") }
  var weightKg by remember { mutableStateOf(initial.weightKg?.toString() ?: "") }
  var goal by remember { mutableStateOf(initial.primaryGoal) }
  var activity by remember { mutableStateOf(initial.activityLevel) }

  Column(
      modifier = modifier.fillMaxSize().background(VColors.Bg).navigationBarsPadding(),
  ) {
    ScreenHeader(step = 0, total = 3, onBack = onBack)
    Title(text = "Tell us about you.", subtitle = "We use this to set baseline targets.")

    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Age slider
      Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          SectionLabel("Age")
          Box(
              modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(VColors.Purple).padding(horizontal = 10.dp, vertical = 4.dp),
          ) {
            Text(text = "$age", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
          }
        }
        Slider(
            value = age.toFloat(),
            onValueChange = { age = it.toInt() },
            valueRange = 13f..100f,
            colors =
                SliderDefaults.colors(
                    thumbColor = VColors.Purple,
                    activeTrackColor = VColors.Purple,
                    inactiveTrackColor = VColors.Card2,
                ),
        )
      }

      // Gender pills
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Gender")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Gender.entries.forEach { g ->
            val active = gender == g
            Box(
                modifier =
                    Modifier.weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) VColors.Purple else VColors.Card)
                        .border(1.dp, if (active) VColors.Purple else VColors.Border, RoundedCornerShape(12.dp))
                        .clickable { gender = g },
                contentAlignment = Alignment.Center,
            ) {
              Text(
                  text = g.label,
                  color = if (active) Color.White else VColors.Ink,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
              )
            }
          }
        }
      }

      // Height + Weight
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VInput(
            value = heightCm,
            onValueChange = { heightCm = it.filter { ch -> ch.isDigit() } },
            label = "Height (cm)",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        VInput(
            value = weightKg,
            onValueChange = { weightKg = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = "Weight (kg)",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
      }

      // Primary goal vertical pills
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Primary goal")
        PrimaryGoal.entries.forEach { g ->
          val active = goal == g
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(48.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(VColors.Card)
                      .border(1.dp, if (active) VColors.Purple else VColors.Border, RoundedCornerShape(12.dp))
                      .clickable { goal = g }
                      .padding(horizontal = 14.dp),
              contentAlignment = Alignment.CenterStart,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Box(
                  modifier =
                      Modifier.size(16.dp)
                          .clip(RoundedCornerShape(99.dp))
                          .background(if (active) VColors.Purple else Color.Transparent)
                          .border(1.5.dp, if (active) VColors.Purple else VColors.InkLo, RoundedCornerShape(99.dp)),
              )
              Text(text = g.label, color = VColors.Ink, style = MaterialTheme.typography.bodyLarge)
            }
          }
        }
      }

      // Activity dropdown (as pills for simplicity)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Activity level")
        ActivityLevel.entries.forEach { lvl ->
          val active = activity == lvl
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(44.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(if (active) VColors.Card2 else VColors.Card)
                      .border(1.dp, if (active) VColors.Purple else VColors.Border, RoundedCornerShape(12.dp))
                      .clickable { activity = lvl }
                      .padding(horizontal = 14.dp),
              contentAlignment = Alignment.CenterStart,
          ) {
            Text(text = lvl.label, color = if (active) VColors.Ink else VColors.InkMd, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)) {
      VPrimaryButton(
          text = "Continue",
          onClick = {
            onContinue(
                initial.copy(
                    age = age,
                    gender = gender,
                    heightCm = heightCm.toIntOrNull(),
                    weightKg = weightKg.toDoubleOrNull(),
                    primaryGoal = goal,
                    activityLevel = activity,
                )
            )
          },
      )
    }
  }
}


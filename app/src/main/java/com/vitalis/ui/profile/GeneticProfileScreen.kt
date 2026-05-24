package com.vitalis.ui.profile

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitalis.profile.ProfileViewModel
import com.vitalis.ui.common.AppHeader
import com.vitalis.ui.common.SectionLabel
import com.vitalis.ui.common.VCard
import com.vitalis.ui.theme.VColors

private data class MockSnp(val gene: String, val trait: String, val genotype: String, val risk: String, val riskColor: Color)

private val mockSnps =
    listOf(
        MockSnp("MTHFR", "Folate metabolism", "C677T", "moderate", VColors.Amber),
        MockSnp("APOE", "Cardiovascular risk", "ε3/ε3", "normal", VColors.Teal),
        MockSnp("LCT", "Lactose tolerance", "-13910 T/C", "elevated", VColors.Red),
        MockSnp("CYP1A2", "Caffeine metabolism", "AC", "moderate", VColors.Amber),
        MockSnp("FTO", "Body weight", "AA", "normal", VColors.Teal),
    )

@Composable
fun GeneticProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel =
        viewModel(factory = ProfileViewModel.Factory((LocalActivity.current as ComponentActivity).application)),
) {
  val profile by profileViewModel.profile.collectAsStateWithLifecycle()

  Column(
      modifier = modifier.fillMaxSize().background(VColors.Bg),
  ) {
    AppHeader(title = "Genetic profile", onBack = onBack, centerTitle = true)

    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // DNA helix illustration
      Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        DnaHelix(modifier = Modifier.size(150.dp))
      }
      Text(
          text =
              "DNA notes guide Vitalis's recommendations. Paste raw or summarised results — Vitalis will weigh them whenever it picks a meal or flags a risk.",
          color = VColors.InkMd,
          style = MaterialTheme.typography.bodyMedium,
      )

      // DNA notes editor
      VCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SectionLabel("DNA NOTES")
          OutlinedTextField(
              value = profile.dnaNotes,
              onValueChange = { v -> profileViewModel.update { it.copy(dnaNotes = v) } },
              modifier = Modifier.fillMaxWidth().height(200.dp),
              placeholder = {
                Text(
                    text =
                        "e.g. MTHFR C677T heterozygous. LCT lactose-intolerant. APOE ε3/ε3. CYP1A2 slow caffeine metaboliser.",
                    color = VColors.InkLo,
                )
              },
              colors =
                  OutlinedTextFieldDefaults.colors(
                      focusedTextColor = VColors.Ink,
                      unfocusedTextColor = VColors.Ink,
                      focusedBorderColor = VColors.Purple,
                      unfocusedBorderColor = VColors.Border,
                      focusedContainerColor = VColors.Card2,
                      unfocusedContainerColor = VColors.Card2,
                      cursorColor = VColors.Purple,
                  ),
              shape = RoundedCornerShape(12.dp),
          )
        }
      }

      // Mock SNP list
      VCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          SectionLabel("DEMO SNP HIGHLIGHTS")
          mockSnps.forEach { snp ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(text = snp.gene, color = VColors.Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = snp.trait, color = VColors.InkMd, style = MaterialTheme.typography.bodySmall)
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(text = snp.genotype, color = VColors.PurpleL, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                Box(
                    modifier =
                        Modifier.clip(RoundedCornerShape(99.dp))
                            .background(snp.riskColor.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                  Text(text = snp.risk, color = snp.riskColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun DnaHelix(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val turns = 4
    val pts1 = mutableListOf<Offset>()
    val pts2 = mutableListOf<Offset>()
    for (i in 0..100) {
      val t = i / 100f
      val y = h * t
      val x1 = w * 0.5f + (w * 0.30f) * kotlin.math.sin((t * turns) * 2 * Math.PI).toFloat()
      val x2 = w * 0.5f + (w * 0.30f) * kotlin.math.sin((t * turns) * 2 * Math.PI + Math.PI).toFloat()
      pts1 += Offset(x1, y)
      pts2 += Offset(x2, y)
    }
    val s = 2.dp.toPx()
    val p1 = Path().apply { moveTo(pts1[0].x, pts1[0].y); pts1.drop(1).forEach { lineTo(it.x, it.y) } }
    val p2 = Path().apply { moveTo(pts2[0].x, pts2[0].y); pts2.drop(1).forEach { lineTo(it.x, it.y) } }
    drawPath(p1, VColors.Purple, style = Stroke(s, cap = StrokeCap.Round))
    drawPath(p2, VColors.Teal, style = Stroke(s, cap = StrokeCap.Round))
    // rungs every 5 points
    for (i in pts1.indices step 5) {
      drawLine(
          color = Color(0x66FFFFFF),
          start = pts1[i],
          end = pts2[i],
          strokeWidth = 1.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f),
      )
    }
  }
}

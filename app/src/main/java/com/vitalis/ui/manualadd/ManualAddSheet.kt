package com.vitalis.ui.manualadd

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitalis.foodlog.FoodDetection
import com.vitalis.ui.common.VOutlineButton
import com.vitalis.ui.common.VPrimaryButton
import com.vitalis.ui.theme.VColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddSheet(
    onDismiss: () -> Unit,
    viewModel: ManualAddViewModel =
        viewModel(
            factory = ManualAddViewModel.Factory((LocalActivity.current as ComponentActivity).application)
        ),
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val state by viewModel.state.collectAsStateWithLifecycle()

  // Auto-dismiss after confirmed.
  LaunchedEffect(state.phase) {
    if (state.phase == ManualAddPhase.CONFIRMED) {
      delay(900)
      viewModel.reset()
      onDismiss()
    }
  }

  ModalBottomSheet(
      onDismissRequest = {
        viewModel.reset()
        onDismiss()
      },
      sheetState = sheetState,
      containerColor = VColors.Card,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
          text = "Log a meal",
          color = VColors.Ink,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
      )
      Text(
          text = "Describe what you ate. Vitalis will estimate macros.",
          color = VColors.InkMd,
          style = MaterialTheme.typography.bodyMedium,
      )

      OutlinedTextField(
          value = state.input,
          onValueChange = viewModel::setInput,
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("e.g. 2 slices pepperoni pizza, 1 medium apple") },
          enabled = state.phase == ManualAddPhase.INPUT || state.phase == ManualAddPhase.ERROR,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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

      when (state.phase) {
        ManualAddPhase.INPUT, ManualAddPhase.ERROR -> {
          VPrimaryButton(
              text = if (state.phase == ManualAddPhase.ERROR) "Try again" else "Estimate macros",
              onClick = viewModel::submit,
              enabled = state.input.isNotBlank(),
          )
          state.errorMessage?.let { msg ->
            Text(text = msg, color = VColors.Red, style = MaterialTheme.typography.bodySmall)
          }
        }
        ManualAddPhase.LOADING -> {
          Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VColors.Purple)
          }
        }
        ManualAddPhase.PREVIEW -> {
          state.preview?.let { PreviewCard(it) }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VOutlineButton(text = "Edit", onClick = { viewModel.reset() }, modifier = Modifier.weight(1f))
            VPrimaryButton(text = "Log it", onClick = viewModel::confirm, modifier = Modifier.weight(1f))
          }
        }
        ManualAddPhase.CONFIRMED -> {
          Text(
              text = "Logged ✓",
              color = VColors.Teal,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun PreviewCard(d: FoodDetection) {
  Box(
      modifier =
          Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(VColors.Card2).padding(14.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (d.isJunk) Text(text = "⚠", color = VColors.Amber, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = d.name.replaceFirstChar { it.uppercase() },
            color = VColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(text = "${d.calories} kcal", color = VColors.Purple, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      }
      Text(
          text = "P ${format(d.proteinG)}g · C ${format(d.carbsG)}g · F ${format(d.fatG)}g${if (d.wasEstimated) " · estimated" else ""}",
          color = VColors.InkMd,
          style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

private fun format(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

package com.vitalis.ui.log

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitalis.foodlog.db.FoodLogEntity
import com.vitalis.ui.common.SectionLabel
import com.vitalis.ui.common.VCard
import com.vitalis.ui.common.VOutlineButton
import com.vitalis.ui.common.VPrimaryButton
import com.vitalis.ui.home.HomeViewModel
import com.vitalis.ui.theme.VColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel =
        viewModel(factory = HomeViewModel.Factory((LocalActivity.current as ComponentActivity).application)),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

  var editing by remember { mutableStateOf<FoodLogEntity?>(null) }
  var deleting by remember { mutableStateOf<FoodLogEntity?>(null) }

  Column(
      modifier = modifier.fillMaxSize().background(VColors.Bg).statusBarsPadding(),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
          text = "Log",
          color = VColors.Ink,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.SemiBold,
      )
      Text(
          text = "${state.summary.totalCalories} kcal",
          color = VColors.PurpleL,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
      )
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (state.entries.isEmpty()) {
        VCard {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("NO MEALS YET")
            Text(
                text = "Log a meal with the + button or your glasses to see it here.",
                color = VColors.InkMd,
                style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      } else {
        state.entries.forEach { entry ->
          FoodLogCard(
              entry = entry,
              timeLabel = timeFmt.format(Date(entry.timestamp)),
              onEdit = { editing = entry },
              onDelete = { deleting = entry },
          )
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  editing?.let { e ->
    EditEntrySheet(
        entry = e,
        onDismiss = { editing = null },
        onSave = { updated ->
          viewModel.updateEntry(updated)
          editing = null
        },
    )
  }

  deleting?.let { e ->
    AlertDialog(
        onDismissRequest = { deleting = null },
        title = { Text("Delete entry?") },
        text = { Text("Remove \"${e.name}\" from your log? Can't be undone.") },
        confirmButton = {
          TextButton(onClick = {
            viewModel.deleteEntry(e.id)
            deleting = null
          }) { Text("Delete", color = VColors.Red) }
        },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun FoodLogCard(
    entry: FoodLogEntity,
    timeLabel: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
  VCard {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = entry.name,
              color = VColors.Ink,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
          )
          Text(
              text = timeLabel,
              color = VColors.InkLo,
              style = MaterialTheme.typography.bodySmall,
          )
        }
        Text(
            text = "${entry.calories} kcal",
            color = VColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
      }
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        MacroChip(label = "P", value = entry.proteinG, color = VColors.Teal, modifier = Modifier.weight(1f))
        MacroChip(label = "C", value = entry.carbsG, color = VColors.Purple, modifier = Modifier.weight(1f))
        MacroChip(label = "F", value = entry.fatG, color = VColors.Amber, modifier = Modifier.weight(1f))
      }
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ActionChip(label = "Edit", color = VColors.PurpleL, onClick = onEdit, modifier = Modifier.weight(1f))
        ActionChip(label = "Delete", color = VColors.Red, onClick = onDelete, modifier = Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun MacroChip(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = "${formatGrams(value)}g",
        color = VColors.InkMd,
        style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun ActionChip(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  androidx.compose.foundation.layout.Box(
      modifier =
          modifier
              .height(38.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(color.copy(alpha = 0.14f))
              .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
  ) {
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEntrySheet(
    entry: FoodLogEntity,
    onDismiss: () -> Unit,
    onSave: (FoodLogEntity) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember(entry.id) { mutableStateOf(entry.name) }
  var calories by remember(entry.id) { mutableStateOf(entry.calories.toString()) }
  var protein by remember(entry.id) { mutableStateOf(formatGrams(entry.proteinG)) }
  var carbs by remember(entry.id) { mutableStateOf(formatGrams(entry.carbsG)) }
  var fat by remember(entry.id) { mutableStateOf(formatGrams(entry.fatG)) }

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      containerColor = VColors.Card,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = "Edit entry",
          color = VColors.Ink,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
      )

      LabeledField(label = "Name", value = name, onValueChange = { name = it })
      LabeledField(
          label = "Calories (kcal)",
          value = calories,
          onValueChange = { calories = it.filter { ch -> ch.isDigit() } },
          keyboardType = KeyboardType.Number,
      )
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LabeledField(
            label = "Protein (g)",
            value = protein,
            onValueChange = { protein = sanitizeDecimal(it) },
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
        )
        LabeledField(
            label = "Carbs (g)",
            value = carbs,
            onValueChange = { carbs = sanitizeDecimal(it) },
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
        )
        LabeledField(
            label = "Fat (g)",
            value = fat,
            onValueChange = { fat = sanitizeDecimal(it) },
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
        )
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VOutlineButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
        VPrimaryButton(
            text = "Save",
            modifier = Modifier.weight(1f),
            enabled = name.isNotBlank() && calories.isNotBlank(),
            onClick = {
              onSave(
                  entry.copy(
                      name = name.trim(),
                      calories = calories.toIntOrNull() ?: entry.calories,
                      proteinG = protein.toDoubleOrNull() ?: entry.proteinG,
                      carbsG = carbs.toDoubleOrNull() ?: entry.carbsG,
                      fatG = fat.toDoubleOrNull() ?: entry.fatG,
                  )
              )
            },
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(text = label, color = VColors.InkMd, style = MaterialTheme.typography.labelMedium)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
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

private fun sanitizeDecimal(s: String): String {
  // Keep digits and a single dot.
  val cleaned = s.filter { it.isDigit() || it == '.' }
  val firstDot = cleaned.indexOf('.')
  return if (firstDot == -1) cleaned
  else cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "")
}

private fun formatGrams(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

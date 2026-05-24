package com.vitalis.ui.glasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vitalis.assistant.MenuRecommendation

@Composable
fun RecommendationDialog(recommendation: MenuRecommendation, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(recommendation.itemName, modifier = Modifier.weight(1f))
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          recommendation.pexelsImageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = recommendation.itemName,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
          }
          Text(text = recommendation.reason, style = MaterialTheme.typography.bodyMedium)
        }
      },
  )
}

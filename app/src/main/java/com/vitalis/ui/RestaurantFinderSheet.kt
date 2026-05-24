package com.vitalis.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vitalis.assistant.RestaurantSearchPhase
import com.vitalis.assistant.RestaurantSearchState
import com.vitalis.placesearch.Restaurant
import com.vitalis.placesearch.TravelMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantFinderSheet(
    state: RestaurantSearchState,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      containerColor = Color(0xFF1A1A1A),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = "Find food nearby",
          color = Color.White,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
      )

      OutlinedTextField(
          value = state.query,
          onValueChange = onQueryChange,
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("e.g. ramen, vegan burrito, sushi") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          enabled = state.phase != RestaurantSearchPhase.LOADING,
      )

      Button(
          onClick = onSubmit,
          modifier = Modifier.fillMaxWidth().height(48.dp),
          enabled =
              state.query.isNotBlank() && state.phase != RestaurantSearchPhase.LOADING,
      ) {
        Icon(Icons.Default.Search, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Search")
      }

      when (state.phase) {
        RestaurantSearchPhase.NEEDS_LOCATION_PERMISSION ->
            PermissionPanel(onGrant = onRequestLocationPermission, message = state.errorMessage)
        RestaurantSearchPhase.LOADING ->
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = Color.White)
            }
        RestaurantSearchPhase.ERROR -> ErrorPanel(message = state.errorMessage)
        RestaurantSearchPhase.RESULTS -> ResultsList(state.results)
        RestaurantSearchPhase.IDLE ->
            Text(
                text = "Type what you're craving, then hit Search. We'll show open spots within 10 km, ordered by how fast you can get there.",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun PermissionPanel(onGrant: () -> Unit, message: String?) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .background(Color(0xFF2A1F00), RoundedCornerShape(12.dp))
              .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
        text = message ?: "We need your location to find nearby restaurants.",
        color = Color(0xFFFFD37D),
        style = MaterialTheme.typography.bodyMedium,
    )
    Button(onClick = onGrant) { Text("Grant location access") }
  }
}

@Composable
private fun ErrorPanel(message: String?) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(Color(0xCC8B0000), RoundedCornerShape(12.dp))
              .padding(16.dp)
  ) {
    Text(
        text = message ?: "Something went wrong.",
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Composable
private fun ResultsList(results: List<Restaurant>) {
  val context = LocalContext.current
  LazyColumn(
      modifier = Modifier.fillMaxSize().heightIn(max = 420.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(results, key = { it.id }) { r ->
      RestaurantRow(
          restaurant = r,
          onClick = {
            val uri =
                Uri.parse(
                    "https://www.google.com/maps/dir/?api=1" +
                        "&destination=${r.latitude},${r.longitude}" +
                        "&destination_place_id=${r.id}" +
                        "&travelmode=${if (r.travel.mode == TravelMode.WALK) "walking" else "driving"}"
                )
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
          },
      )
    }
  }
}

@Composable
private fun RestaurantRow(restaurant: Restaurant, onClick: () -> Unit) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .clickable(onClick = onClick)
              .background(Color(0xFF252525), RoundedCornerShape(12.dp))
              .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
          text = restaurant.name,
          color = Color.White,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
      )
      restaurant.rating?.let { rating ->
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFC857),
            modifier = Modifier.height(14.dp),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "%.1f".format(rating),
            color = Color(0xFFFFC857),
            style = MaterialTheme.typography.labelMedium,
        )
        restaurant.ratingCount?.let { count ->
          Text(
              text = "($count)",
              color = Color.White.copy(alpha = 0.55f),
              style = MaterialTheme.typography.labelSmall,
          )
        }
      }
    }
    Text(
        text = restaurant.address,
        color = Color.White.copy(alpha = 0.7f),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
          imageVector =
              if (restaurant.travel.mode == TravelMode.WALK) Icons.Default.DirectionsWalk
              else Icons.Default.DirectionsCar,
          contentDescription = null,
          tint = Color(0xFF7DD3A0),
          modifier = Modifier.height(16.dp),
      )
      Text(
          text = restaurant.travel.formatted(),
          color = Color(0xFF7DD3A0),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
      )
      restaurant.priceLevel?.let { priceLabel(it)?.let { p -> DotText(p) } }
      restaurant.openNow?.let { open ->
        DotText(if (open) "Open" else "Closed", color = if (open) Color(0xFF7DD3A0) else Color(0xFFFF8A8A))
      }
    }
  }
}

@Composable
private fun DotText(text: String, color: Color = Color.White.copy(alpha = 0.6f)) {
  Text(text = "·", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelMedium)
  Text(text = text, color = color, style = MaterialTheme.typography.labelMedium)
}

private fun priceLabel(level: String): String? =
    when (level) {
      "PRICE_LEVEL_FREE" -> "Free"
      "PRICE_LEVEL_INEXPENSIVE" -> "$"
      "PRICE_LEVEL_MODERATE" -> "$$"
      "PRICE_LEVEL_EXPENSIVE" -> "$$$"
      "PRICE_LEVEL_VERY_EXPENSIVE" -> "$$$$"
      else -> null
    }

package com.vitalis.ui

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vitalis.assistant.AssistantPhase
import com.vitalis.assistant.AssistantViewModel
import com.vitalis.assistant.MenuRecommendation
import com.vitalis.stream.StreamViewModel
import com.vitalis.voice.VoicePhase
import com.vitalis.wearables.WearablesViewModel
import kotlin.math.min

@Composable
fun AssistantScreen(
    wearablesViewModel: WearablesViewModel,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    streamViewModel: StreamViewModel =
        viewModel(
            factory =
                StreamViewModel.Factory(
                    application = (LocalActivity.current as ComponentActivity).application,
                    wearablesViewModel = wearablesViewModel,
                )
        ),
    assistantViewModel: AssistantViewModel =
        viewModel(
            factory =
                AssistantViewModel.Factory(
                    (LocalActivity.current as ComponentActivity).application
                )
        ),
) {
  val streamState by streamViewModel.uiState.collectAsStateWithLifecycle()
  val assistantState by assistantViewModel.uiState.collectAsStateWithLifecycle()
  val recentLog by assistantViewModel.recentFoodLog.collectAsStateWithLifecycle()

  val locationPermissionLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions(),
      ) { results ->
        val granted =
            results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
          assistantViewModel.onLocationPermissionGranted()
          assistantViewModel.runRestaurantSearch()
        } else {
          assistantViewModel.onLocationPermissionDenied()
        }
      }

  // Start the live stream + passive food logging when this screen is composed.
  // Also expose captureStill to the VM so the voice agent can trigger a menu scan.
  DisposableEffect(Unit) {
    streamViewModel.startStream()
    assistantViewModel.startFoodLogging()
    assistantViewModel.captureStillProvider = { streamViewModel.captureMenuStill() }
    onDispose {
      assistantViewModel.captureStillProvider = null
      assistantViewModel.stopFoodLogging()
      streamViewModel.stopStream()
    }
  }

  // RECORD_AUDIO launcher — request just-in-time when the mic is tapped.
  val recordAudioLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
      ) { granted ->
        if (granted) assistantViewModel.startVoiceTurn()
        // If denied, the VoiceState will stay IDLE; user can retry from settings.
      }

  // Push the latest video frame into the assistant for sampling.
  LaunchedEffect(streamState.videoFrame) {
    assistantViewModel.updateLatestFrame(streamState.videoFrame)
  }

  // Stop the stream once a menu still is captured; restart it when we return to logging.
  LaunchedEffect(assistantState.phase) {
    when (assistantState.phase) {
      AssistantPhase.MENU_ANALYZING,
      AssistantPhase.MENU_RESULTS -> streamViewModel.stopStream()
      AssistantPhase.LOGGING,
      AssistantPhase.ERROR -> streamViewModel.startStream()
      AssistantPhase.MENU_SCANNING -> {}
    }
  }

  MaterialTheme(colorScheme = darkColorScheme()) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

      // --- Background: live stream or frozen menu ---
      when (assistantState.phase) {
        AssistantPhase.MENU_ANALYZING,
        AssistantPhase.MENU_RESULTS -> {
          assistantState.capturedMenu?.let { menu ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
              val containerW = constraints.maxWidth.toFloat()
              val containerH = constraints.maxHeight.toFloat()
              val bmW = menu.width.toFloat()
              val bmH = menu.height.toFloat()
              val scale = min(containerW / bmW, containerH / bmH)
              val offsetX = (containerW - bmW * scale) / 2f
              val offsetY = (containerH - bmH * scale) / 2f

              Image(
                  bitmap = menu.asImageBitmap(),
                  contentDescription = "Detected menu",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Fit,
              )

              if (assistantState.phase == AssistantPhase.MENU_RESULTS) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                assistantState.recommendations.forEach { rec ->
                  val cx = offsetX + rec.box.centerX() * scale
                  val cy = offsetY + rec.box.centerY() * scale
                  val xDp = with(density) { cx.toDp() }
                  val yDp = with(density) { cy.toDp() }
                  PulsatingDot(
                      onClick = { assistantViewModel.selectRecommendation(rec.id) },
                      modifier = Modifier.offset(x = xDp - 14.dp, y = yDp - 14.dp).size(28.dp),
                  )
                }
              }
            }
          }
        }
        else -> {
          streamState.videoFrame?.let { frame ->
            key(streamState.videoFrameCount) {
              Image(
                  bitmap = frame.asImageBitmap(),
                  contentDescription = "Live stream",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop,
              )
            }
          }
              ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Connecting to glasses…",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
              }
        }
      }

      // --- Top bar ---
      Row(
          modifier =
              Modifier.align(Alignment.TopCenter)
                  .fillMaxWidth()
                  .statusBarsPadding()
                  .padding(horizontal = 8.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
            text = "Assistant",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onOpenSettings) {
          Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }
      }

      // --- Bottom: food log overlay (when logging) + voice overlay + phase-specific control ---
      val context = androidx.compose.ui.platform.LocalContext.current
      Column(
          modifier =
              Modifier.align(Alignment.BottomCenter)
                  .fillMaxWidth()
                  .navigationBarsPadding()
                  .padding(horizontal = 16.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (assistantState.phase == AssistantPhase.LOGGING ||
            assistantState.phase == AssistantPhase.ERROR) {
          FoodLogOverlay(entries = recentLog)
        }

        assistantState.statusMessage?.let { StatusPill(text = it) }

        // Voice overlay is rendered whenever the voice state isn't fully idle.
        if (assistantState.voice.phase != VoicePhase.IDLE ||
            !assistantState.voice.response.isNullOrBlank()) {
          VoiceOverlay(
              state = assistantState.voice,
              onDismiss = {
                assistantViewModel.cancelVoice()
                assistantViewModel.dismissVoiceOverlay()
              },
          )
        }

        when (assistantState.phase) {
          AssistantPhase.LOGGING -> {
            // Single mic button replaces the old Scan menu + Find food row.
            MicButton(
                listening = assistantState.voice.phase == VoicePhase.LISTENING,
                onClick = {
                  if (assistantState.voice.phase == VoicePhase.LISTENING ||
                      assistantState.voice.phase == VoicePhase.THINKING) {
                    assistantViewModel.cancelVoice()
                  } else {
                    val granted =
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (granted) {
                      assistantViewModel.startVoiceTurn()
                    } else {
                      recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  }
                },
            )
          }
          AssistantPhase.MENU_SCANNING -> {
            Button(onClick = { assistantViewModel.cancelMenuScanning() }) {
              Text("Stop scanning")
            }
          }
          AssistantPhase.MENU_ANALYZING -> {
            CircularProgressIndicator(color = Color.White)
          }
          AssistantPhase.MENU_RESULTS -> {
            Button(onClick = { assistantViewModel.resetMenuToLogging() }) {
              Text("Back to logging")
            }
          }
          AssistantPhase.ERROR -> {
            assistantState.errorMessage?.let { msg ->
              Text(
                  text = msg,
                  color = Color.White,
                  modifier =
                      Modifier.clip(RoundedCornerShape(16.dp))
                          .background(Color(0xCC8B0000))
                          .padding(horizontal = 16.dp, vertical = 12.dp),
              )
            }
            Button(onClick = { assistantViewModel.retryFromError() }) { Text("Try again") }
          }
        }
      }

      // --- Detail dialog for selected menu recommendation ---
      val selectedId = assistantState.selectedRecommendationId
      if (selectedId != null) {
        val selected = assistantState.recommendations.firstOrNull { it.id == selectedId }
        if (selected != null) {
          RecommendationDialog(
              recommendation = selected,
              onDismiss = { assistantViewModel.selectRecommendation(null) },
          )
        }
      }

      // --- Restaurant finder sheet ---
      if (assistantState.restaurantSearch.visible) {
        RestaurantFinderSheet(
            state = assistantState.restaurantSearch,
            onQueryChange = { assistantViewModel.setRestaurantQuery(it) },
            onSubmit = { assistantViewModel.runRestaurantSearch() },
            onRequestLocationPermission = {
              locationPermissionLauncher.launch(
                  arrayOf(
                      Manifest.permission.ACCESS_FINE_LOCATION,
                      Manifest.permission.ACCESS_COARSE_LOCATION,
                  )
              )
            },
            onDismiss = { assistantViewModel.closeRestaurantSearch() },
        )
      }
    }
  }
}

@Composable
private fun StatusPill(text: String) {
  Box(
      modifier =
          Modifier.clip(RoundedCornerShape(24.dp))
              .background(Color(0xAA000000))
              .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Text(text = text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun PulsatingDot(onClick: () -> Unit, modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "pulse")
  val scale by
      transition.animateFloat(
          initialValue = 0.85f,
          targetValue = 1.15f,
          animationSpec =
              infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
          label = "scale",
      )
  val alpha by
      transition.animateFloat(
          initialValue = 0.55f,
          targetValue = 0.95f,
          animationSpec =
              infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
          label = "alpha",
      )

  Box(
      modifier =
          modifier
              .scale(scale)
              .clip(CircleShape)
              .background(Color(0xFFFFC857).copy(alpha = alpha))
              .clickable(onClick = onClick)
  )
}

@Composable
private fun RecommendationDialog(recommendation: MenuRecommendation, onDismiss: () -> Unit) {
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
                modifier =
                    Modifier.fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
          }
          Text(text = recommendation.reason, style = MaterialTheme.typography.bodyMedium)
        }
      },
  )
}

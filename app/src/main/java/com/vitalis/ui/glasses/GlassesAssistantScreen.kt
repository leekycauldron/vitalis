package com.vitalis.ui.glasses

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitalis.assistant.AssistantPhase
import com.vitalis.assistant.AssistantViewModel
import com.vitalis.stream.StreamViewModel
import com.vitalis.ui.FoodLogOverlay
import com.vitalis.ui.MicButton
import com.vitalis.ui.RestaurantFinderSheet
import com.vitalis.ui.VoiceOverlay
import com.vitalis.ui.common.BackChevron
import com.vitalis.ui.common.PulsatingDot
import com.vitalis.ui.common.VPrimaryButton
import com.vitalis.ui.common.Wordmark
import com.vitalis.ui.theme.VColors
import com.vitalis.voice.VoicePhase
import com.vitalis.wearables.WearablesViewModel
import kotlin.math.min

@Composable
fun GlassesAssistantScreen(
    wearablesViewModel: WearablesViewModel,
    onBack: () -> Unit,
    onOpenManualAdd: () -> Unit,
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
  val context = androidx.compose.ui.platform.LocalContext.current

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

  LaunchedEffect(streamState.videoFrame) {
    assistantViewModel.updateLatestFrame(streamState.videoFrame)
  }

  LaunchedEffect(assistantState.phase) {
    when (assistantState.phase) {
      AssistantPhase.MENU_ANALYZING,
      AssistantPhase.MENU_RESULTS -> streamViewModel.stopStream()
      AssistantPhase.LOGGING,
      AssistantPhase.ERROR -> streamViewModel.startStream()
      AssistantPhase.MENU_SCANNING -> {}
    }
  }

  val recordAudioLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
      ) { granted ->
        if (granted) assistantViewModel.startVoiceTurn()
      }

  Box(modifier = modifier.fillMaxSize().background(VColors.Bg)) {

    // Background: live stream or frozen menu
    when (assistantState.phase) {
      AssistantPhase.MENU_ANALYZING,
      AssistantPhase.MENU_RESULTS ->
          assistantState.capturedMenu?.let { menu ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
              val cw = constraints.maxWidth.toFloat()
              val ch = constraints.maxHeight.toFloat()
              val scale = min(cw / menu.width.toFloat(), ch / menu.height.toFloat())
              val ox = (cw - menu.width * scale) / 2f
              val oy = (ch - menu.height * scale) / 2f
              Image(
                  bitmap = menu.asImageBitmap(),
                  contentDescription = "Detected menu",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Fit,
              )
              if (assistantState.phase == AssistantPhase.MENU_RESULTS) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                assistantState.recommendations.forEach { rec ->
                  val cx = ox + rec.box.centerX() * scale
                  val cy = oy + rec.box.centerY() * scale
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
                  color = VColors.InkMd,
                  style = MaterialTheme.typography.bodyMedium,
              )
            }
      }
    }

    // Top bar with Wordmark + status + back
    Row(
        modifier =
            Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Box(
          modifier =
              Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                  .background(Color(0x66000000))
                  .border(1.dp, VColors.Border, RoundedCornerShape(12.dp))
                  .clickable(onClick = onBack),
          contentAlignment = Alignment.Center,
      ) {
        BackChevron(color = VColors.Ink)
      }
      Wordmark()
      StatusPill(streaming = streamState.videoFrame != null && assistantState.phase != AssistantPhase.MENU_ANALYZING && assistantState.phase != AssistantPhase.MENU_RESULTS)
    }

    // Bottom bar
    Column(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (assistantState.phase == AssistantPhase.LOGGING || assistantState.phase == AssistantPhase.ERROR) {
        FoodLogOverlay(entries = recentLog)
      }

      assistantState.statusMessage?.let { msg -> StatusToast(text = msg) }

      if (assistantState.voice.phase != VoicePhase.IDLE || !assistantState.voice.response.isNullOrBlank()) {
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
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            // Manual-add quick button (left)
            Box(
                modifier =
                    Modifier.size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xCC10162A))
                        .border(1.dp, VColors.BorderPurple, RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenManualAdd),
                contentAlignment = Alignment.Center,
            ) {
              Text(text = "+", color = VColors.PurpleL, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
                      if (granted) assistantViewModel.startVoiceTurn()
                      else recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  },
              )
            }

            // Spacer matches the manual-add button so the mic stays centered.
            Box(modifier = Modifier.size(52.dp))
          }
        }
        AssistantPhase.MENU_SCANNING -> VPrimaryButton(text = "Stop scanning", onClick = { assistantViewModel.cancelMenuScanning() })
        AssistantPhase.MENU_ANALYZING -> CircularProgressIndicator(color = VColors.PurpleL)
        AssistantPhase.MENU_RESULTS -> VPrimaryButton(text = "Back to logging", onClick = { assistantViewModel.resetMenuToLogging() })
        AssistantPhase.ERROR -> {
          assistantState.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = Color.White,
                modifier =
                    Modifier.clip(RoundedCornerShape(14.dp))
                        .background(Color(0xCC8B0000))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            )
          }
          VPrimaryButton(text = "Try again", onClick = { assistantViewModel.retryFromError() })
        }
      }
    }

    // Menu recommendation detail dialog
    val selectedId = assistantState.selectedRecommendationId
    if (selectedId != null) {
      val sel = assistantState.recommendations.firstOrNull { it.id == selectedId }
      if (sel != null) {
        RecommendationDialog(
            recommendation = sel,
            onDismiss = { assistantViewModel.selectRecommendation(null) },
        )
      }
    }

    if (assistantState.restaurantSearch.visible) {
      RestaurantFinderSheet(
          state = assistantState.restaurantSearch,
          onQueryChange = { assistantViewModel.setRestaurantQuery(it) },
          onSubmit = { assistantViewModel.runRestaurantSearch() },
          onRequestLocationPermission = { /* handled in screen; for voice-triggered the sheet shows prompt */ },
          onDismiss = { assistantViewModel.closeRestaurantSearch() },
      )
    }
  }
}

@Composable
private fun StatusPill(streaming: Boolean) {
  Row(
      modifier =
          Modifier.clip(RoundedCornerShape(999.dp))
              .background(Color(0xCC000000))
              .border(1.dp, VColors.Border, RoundedCornerShape(999.dp))
              .padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
        modifier =
            Modifier.size(7.dp).clip(RoundedCornerShape(99.dp))
                .background(if (streaming) VColors.Red else VColors.Teal),
    )
    Text(
        text = if (streaming) "LIVE" else "PAUSED",
        color = if (streaming) VColors.Red else VColors.Teal,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun StatusToast(text: String) {
  Box(
      modifier =
          Modifier.clip(RoundedCornerShape(24.dp))
              .background(Color(0xCC000000))
              .border(1.dp, VColors.BorderPurple, RoundedCornerShape(24.dp))
              .padding(horizontal = 16.dp, vertical = 10.dp),
  ) {
    Text(text = text, color = VColors.Ink, style = MaterialTheme.typography.bodyMedium)
  }
}

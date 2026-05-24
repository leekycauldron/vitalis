package com.vitalis.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.vitalis.ui.common.BottomNav
import com.vitalis.ui.common.VTab
import com.vitalis.ui.glasses.GlassesAssistantScreen
import com.vitalis.ui.home.HomeDashboardScreen
import com.vitalis.ui.insights.InsightsHubScreen
import com.vitalis.ui.manualadd.ManualAddSheet
import com.vitalis.ui.profile.GeneticProfileScreen
import com.vitalis.ui.profile.ProfileScreen
import com.vitalis.ui.theme.VColors
import com.vitalis.ui.trends.TrendsHubScreen
import com.vitalis.wearables.WearablesViewModel

/**
 * Tabbed application shell with bottom navigation. Holds:
 * - Five tabs (Home / Trends / + / Insights / Profile).
 * - The manual-add bottom sheet (triggered by + and from anywhere via [showManualAdd]).
 * - The glasses sub-flow as a fullscreen overlay route from Home or Profile.
 */
@Composable
fun TabbedShell(
    wearablesViewModel: WearablesViewModel,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    modifier: Modifier = Modifier,
) {
  val wearablesState by wearablesViewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  var tab by remember { mutableStateOf(VTab.Home) }
  var manualAddOpen by remember { mutableStateOf(false) }
  var geneticOpen by remember { mutableStateOf(false) }
  val activity = LocalActivity.current as? ComponentActivity

  val glassesAction: () -> Unit = remember(wearablesState.isRegistered, wearablesState.hasActiveDevice) {
    {
      if (!wearablesState.isRegistered) {
        activity?.let { wearablesViewModel.startRegistration(it) }
      } else {
        wearablesViewModel.navigateToAssistant(onRequestWearablesPermission)
      }
    }
  }

  LaunchedEffect(wearablesState.recentError) {
    wearablesState.recentError?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      wearablesViewModel.clearRecentError()
    }
  }

  Box(modifier = modifier.fillMaxSize().background(VColors.Bg)) {
    when {
      wearablesState.isAssistantMode -> {
        GlassesAssistantScreen(
            wearablesViewModel = wearablesViewModel,
            onBack = { wearablesViewModel.exitAssistant() },
            onOpenManualAdd = { manualAddOpen = true },
        )
      }
      geneticOpen -> {
        GeneticProfileScreen(onBack = { geneticOpen = false })
      }
      else -> {
        Column(modifier = Modifier.fillMaxSize()) {
          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
              VTab.Home ->
                  HomeDashboardScreen(
                      wearablesState = wearablesState,
                      onOpenGlasses = glassesAction,
                      onOpenGenetic = { geneticOpen = true },
                      onOpenManualAdd = { manualAddOpen = true },
                  )
              VTab.Trends -> TrendsHubScreen()
              VTab.Add -> Box {} // sheet handles it
              VTab.Insights -> InsightsHubScreen()
              VTab.Profile ->
                  ProfileScreen(
                      onOpenGenetic = { geneticOpen = true },
                      onOpenGlasses = glassesAction,
                  )
            }
          }
          BottomNav(
              active = tab,
              onSelect = { selected ->
                if (selected == VTab.Add) manualAddOpen = true
                else tab = selected
              },
          )
        }
      }
    }

    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
  }

  if (manualAddOpen) {
    ManualAddSheet(onDismiss = { manualAddOpen = false })
  }
}

package com.vitalis.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.vitalis.profile.ProfileViewModel
import com.vitalis.ui.onboarding.OnboardingFlow
import com.vitalis.ui.shell.TabbedShell
import com.vitalis.ui.theme.VColors
import com.vitalis.ui.theme.VTheme
import com.vitalis.wearables.WearablesViewModel

/**
 * Top-level Vitalis root. Replaces the old `VitalisScaffold`. Decides between onboarding and the
 * tabbed app shell based on the persisted profile.
 *
 * Glasses-specific permission flow ([WearablesViewModel.navigateToAssistant]) is still owned by
 * the wearables VM; the tabbed shell wires it up when the user enters the glasses sub-flow.
 */
@Composable
fun AppRoot(
    wearablesViewModel: WearablesViewModel,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel =
        viewModel(
            factory =
                ProfileViewModel.Factory(
                    (LocalActivity.current as ComponentActivity).application
                )
        ),
) {
  val profile by profileViewModel.profile.collectAsStateWithLifecycle()

  VTheme {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Box(modifier = Modifier.fillMaxSize().background(VColors.Bg)) {
        if (!profile.onboardingComplete) {
          OnboardingFlow(onComplete = { /* recomposition triggered by repo flow */ })
        } else {
          TabbedShell(
              wearablesViewModel = wearablesViewModel,
              onRequestWearablesPermission = onRequestWearablesPermission,
          )
        }
      }
    }
  }
}

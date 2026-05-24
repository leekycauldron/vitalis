package com.vitalis.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitalis.profile.ProfileViewModel
import com.vitalis.profile.UserProfile

private enum class Step { Welcome, CreateAccount, AboutYou, FocusAreas, AllSet }

@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    profileViewModel: ProfileViewModel =
        viewModel(
            factory =
                ProfileViewModel.Factory(
                    (LocalActivity.current as ComponentActivity).application
                )
        ),
) {
  val storedProfile by profileViewModel.profile.collectAsStateWithLifecycle()
  var step by remember { mutableStateOf(Step.Welcome) }
  var draft by remember { mutableStateOf(UserProfile()) }

  // Seed the draft from any stored profile (e.g. if reset interrupted).
  remember(storedProfile) {
    if (draft == UserProfile()) draft = storedProfile.copy(onboardingComplete = false)
    Unit
  }

  when (step) {
    Step.Welcome ->
        WelcomeScreen(
            onCreateAccount = { step = Step.CreateAccount },
            onLogIn = { step = Step.CreateAccount },
        )
    Step.CreateAccount ->
        CreateAccountScreen(
            initialEmail = draft.email,
            onBack = { step = Step.Welcome },
            onCreate = { email, name ->
              draft = draft.copy(email = email, name = name)
              step = Step.AboutYou
            },
        )
    Step.AboutYou ->
        AboutYouScreen(
            initial = draft,
            onBack = { step = Step.CreateAccount },
            onContinue = { updated ->
              draft = updated
              step = Step.FocusAreas
            },
        )
    Step.FocusAreas ->
        FocusAreasScreen(
            initial = draft.focusAreas,
            onBack = { step = Step.AboutYou },
            onContinue = { selected ->
              draft = draft.copy(focusAreas = selected)
              step = Step.AllSet
            },
        )
    Step.AllSet ->
        AllSetScreen(
            nameOrEmail = draft.name.ifBlank { draft.email },
            onGetStarted = {
              val finalProfile = draft.copy(onboardingComplete = true)
              profileViewModel.update { finalProfile }
              onComplete()
            },
        )
  }
}

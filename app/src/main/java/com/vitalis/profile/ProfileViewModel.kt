package com.vitalis.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

  private val repo = ProfileRepository.get(application)

  val profile: StateFlow<UserProfile> =
      repo.profile.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000L),
          initialValue = UserProfile(),
      )

  fun update(transform: (UserProfile) -> UserProfile) {
    viewModelScope.launch { repo.update(transform) }
  }

  fun completeOnboarding() {
    viewModelScope.launch { repo.update { it.copy(onboardingComplete = true) } }
  }

  fun resetProfile() {
    viewModelScope.launch { repo.reset() }
  }

  class Factory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
      @Suppress("UNCHECKED_CAST") return ProfileViewModel(application) as T
    }
  }
}

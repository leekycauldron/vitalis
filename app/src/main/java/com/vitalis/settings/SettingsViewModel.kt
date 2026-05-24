package com.vitalis.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vitalis.foodlog.FoodLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

  private val repo = SettingsRepository(application)
  private val foodLogRepo = FoodLogRepository.create(application)

  val settings: StateFlow<VitalisSettings> =
      repo.settings.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000L),
          initialValue = VitalisSettings(),
      )

  val foodLogEntryCount: StateFlow<Int> =
      foodLogRepo
          .observeRecent(limit = Int.MAX_VALUE)
          .map { it.size }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5_000L),
              initialValue = 0,
          )

  fun setPersonalProfile(profile: String) {
    viewModelScope.launch { repo.update { it.copy(personalProfile = profile) } }
  }

  fun setDietaryAvoidance(avoidance: String) {
    viewModelScope.launch { repo.update { it.copy(dietaryAvoidance = avoidance) } }
  }

  fun clearFoodLog() {
    viewModelScope.launch { foodLogRepo.clearAll() }
  }

  class Factory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
      @Suppress("UNCHECKED_CAST") return SettingsViewModel(application) as T
    }
  }
}

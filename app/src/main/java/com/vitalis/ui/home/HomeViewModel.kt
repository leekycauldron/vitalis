package com.vitalis.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vitalis.foodlog.FoodLogRepository
import com.vitalis.foodlog.FoodLogSummary
import com.vitalis.foodlog.db.FoodLogEntity
import com.vitalis.profile.MacroBalance
import com.vitalis.profile.MacroTargets
import com.vitalis.profile.ProfileRepository
import com.vitalis.profile.UserProfile
import java.util.Calendar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class HomeDashboardState(
    val profile: UserProfile = UserProfile(),
    val summary: FoodLogSummary = FoodLogSummary(0, 0, 0.0, 0.0, 0.0, 0, emptyList()),
    val targets: MacroTargets = MacroTargets.Default,
    val balance: MacroBalance =
        MacroBalance.compute(FoodLogSummary(0, 0, 0.0, 0.0, 0.0, 0, emptyList()), MacroTargets.Default),
    val entries: List<FoodLogEntity> = emptyList(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

  private val foodLogRepo = FoodLogRepository.create(application)
  private val profileRepo = ProfileRepository.get(application)

  // Re-compute today's summary on a periodic tick (60s) so the dashboard stays fresh even
  // without a flow-based DAO query.
  private val tick = flow {
    while (true) {
      emit(Unit)
      delay(60_000L)
    }
  }

  val state: StateFlow<HomeDashboardState> =
      combine(profileRepo.profile, foodLogRepo.observeRecent(limit = 500), tick) { profile, recent, _ ->
        val targets = MacroTargets.fromProfile(profile)
        val startMs = startOfTodayMs()
        val summary = foodLogRepo.summarize(startMs)
        val balance = MacroBalance.compute(summary, targets)
        val todayEntries = recent.filter { it.timestamp >= startMs }
        HomeDashboardState(
            profile = profile,
            summary = summary,
            targets = targets,
            balance = balance,
            entries = todayEntries,
        )
      }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), HomeDashboardState())

  fun deleteEntry(id: Long) {
    viewModelScope.launch { foodLogRepo.deleteEntry(id) }
  }

  fun updateEntry(entry: FoodLogEntity) {
    viewModelScope.launch { foodLogRepo.updateEntry(entry) }
  }

  private fun startOfTodayMs(): Long {
    val c = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
  }

  class Factory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(HomeViewModel::class.java))
      @Suppress("UNCHECKED_CAST") return HomeViewModel(application) as T
    }
  }
}

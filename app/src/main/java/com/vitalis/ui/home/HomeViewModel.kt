package com.vitalis.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vitalis.foodlog.FoodLogRepository
import com.vitalis.foodlog.FoodLogSummary
import com.vitalis.profile.MacroBalance
import com.vitalis.profile.MacroTargets
import com.vitalis.profile.ProfileRepository
import com.vitalis.profile.UserProfile
import java.util.Calendar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay

data class HomeDashboardState(
    val profile: UserProfile = UserProfile(),
    val summary: FoodLogSummary = FoodLogSummary(0, 0, 0.0, 0.0, 0.0, 0, emptyList()),
    val targets: MacroTargets = MacroTargets.Default,
    val balance: MacroBalance =
        MacroBalance.compute(FoodLogSummary(0, 0, 0.0, 0.0, 0.0, 0, emptyList()), MacroTargets.Default),
    val healthScore: Int = 72,
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
      combine(profileRepo.profile, foodLogRepo.observeRecent(limit = 500), tick) { profile, _, _ ->
        val targets = MacroTargets.fromProfile(profile)
        val summary = foodLogRepo.summarize(startOfTodayMs())
        val balance = MacroBalance.compute(summary, targets)
        HomeDashboardState(
            profile = profile,
            summary = summary,
            targets = targets,
            balance = balance,
            healthScore = computeHealthScore(balance),
        )
      }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), HomeDashboardState())

  private fun computeHealthScore(b: MacroBalance): Int {
    // Cheap derived score: 100 minus penalties for imbalances.
    val penalty = b.imbalances.size * 7
    return (100 - penalty).coerceIn(40, 100)
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

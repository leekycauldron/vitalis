package com.vitalis.ui.manualadd

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vitalis.BuildConfig
import com.vitalis.foodlog.FoodDetection
import com.vitalis.foodlog.FoodLogRepository
import com.vitalis.foodlog.FoodsCsvLoader
import com.vitalis.foodlog.ManualFoodLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "Vitalis:ManualAddVM"

enum class ManualAddPhase { INPUT, LOADING, PREVIEW, CONFIRMED, ERROR }

data class ManualAddState(
    val phase: ManualAddPhase = ManualAddPhase.INPUT,
    val input: String = "",
    val preview: FoodDetection? = null,
    val errorMessage: String? = null,
)

class ManualAddViewModel(application: Application) : AndroidViewModel(application) {

  private val foodLogRepo = FoodLogRepository.create(application)
  private val knownFoods = FoodsCsvLoader.load(application)
  private val logger = ManualFoodLogger(BuildConfig.ANTHROPIC_API_KEY, knownFoods)

  private val _state = MutableStateFlow(ManualAddState())
  val state: StateFlow<ManualAddState> = _state.asStateFlow()

  fun setInput(text: String) {
    _state.update { it.copy(input = text, errorMessage = null) }
  }

  fun submit() {
    val text = _state.value.input.trim()
    if (text.isBlank()) return
    if (BuildConfig.ANTHROPIC_API_KEY.isBlank()) {
      _state.update {
        it.copy(
            phase = ManualAddPhase.ERROR,
            errorMessage = "Add an ANTHROPIC_API_KEY in local.properties and rebuild.",
        )
      }
      return
    }
    _state.update { it.copy(phase = ManualAddPhase.LOADING, errorMessage = null) }
    viewModelScope.launch {
      val parsed = runCatching { logger.parse(text) }.onFailure { Log.e(TAG, "Parse failed", it) }.getOrNull()
      if (parsed == null) {
        _state.update {
          it.copy(
              phase = ManualAddPhase.ERROR,
              errorMessage = "Couldn't estimate macros for that. Try a different wording.",
          )
        }
      } else {
        _state.update { it.copy(phase = ManualAddPhase.PREVIEW, preview = parsed) }
      }
    }
  }

  fun confirm() {
    val detection = _state.value.preview ?: return
    viewModelScope.launch {
      val logged = foodLogRepo.tryLog(detection)
      _state.update {
        if (logged) {
          it.copy(phase = ManualAddPhase.CONFIRMED, errorMessage = null)
        } else {
          it.copy(
              phase = ManualAddPhase.ERROR,
              errorMessage = "That item was already logged in the last minute.",
          )
        }
      }
    }
  }

  fun reset() {
    _state.update { ManualAddState() }
  }

  class Factory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(ManualAddViewModel::class.java))
      @Suppress("UNCHECKED_CAST") return ManualAddViewModel(application) as T
    }
  }
}

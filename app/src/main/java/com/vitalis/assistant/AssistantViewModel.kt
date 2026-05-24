package com.vitalis.assistant

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vitalis.BuildConfig
import com.vitalis.foodlog.FoodDetection
import com.vitalis.foodlog.FoodLogRepository
import com.vitalis.foodlog.FoodsCsvLoader
import com.vitalis.foodlog.db.FoodLogEntity
import com.vitalis.placesearch.LocationProvider
import com.vitalis.placesearch.PlacesClient
import com.vitalis.profile.MacroBalance
import com.vitalis.profile.MacroTargets
import com.vitalis.profile.ProfileRepository
import com.vitalis.profile.PromptContext
import com.vitalis.profile.UserProfile
import com.vitalis.settings.SettingsRepository
import com.vitalis.settings.VitalisSettings
import com.vitalis.voice.VoiceController
import com.vitalis.voice.VoiceState
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "Vitalis:AssistantViewModel"
private const val MENU_SAMPLE_INTERVAL_MS = 3_000L
private const val FOOD_SAMPLE_INTERVAL_MS = 3_000L
private const val ROAST_COOLDOWN_MS = 60_000L

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

  private val settingsRepo = SettingsRepository(application)
  private val profileRepo = ProfileRepository.get(application)
  private val knownFoods = FoodsCsvLoader.load(application)
  private val foodLogRepo = FoodLogRepository.create(application)
  private val audio = AudioPlayback(application)
  private val locationProvider = LocationProvider(application)

  private suspend fun buildPromptContext(): PromptContext {
    val profile = profileRepo.profile.first()
    val targets = MacroTargets.fromProfile(profile)
    val summary = foodLogRepo.summarize(startOfTodayMs())
    val balance = MacroBalance.compute(summary, targets)
    val recent = foodLogRepo.recentlySeenLabels()
    return PromptContext(profile = profile, macroBalance = balance, recentLabels = recent)
  }

  private fun startOfTodayMs(): Long {
    val c = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
  }

  /**
   * Set by [AssistantScreen] inside a DisposableEffect — gives the voice controller a way to
   * trigger a high-res still capture without needing a direct StreamViewModel handle. Null when
   * the stream isn't running.
   */
  @Volatile var captureStillProvider: (suspend () -> android.graphics.Bitmap?)? = null

  private val _uiState = MutableStateFlow(AssistantUiState())
  val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

  private val voiceController =
      VoiceController(
          context = application,
          audio = audio,
          foodLog = foodLogRepo,
          publishState = { newState -> _uiState.update { it.copy(voice = newState) } },
          onStartMenuScan = { preference ->
            val provider = captureStillProvider
            if (provider != null) startMenuScanningWithPreference(preference, provider)
            else
                Log.w(TAG, "Voice menu-scan requested but stream isn't active yet — ignoring")
          },
          onFindRestaurant = { query -> runRestaurantSearchWithQuery(query) },
      )

  val settings: StateFlow<VitalisSettings> =
      settingsRepo.settings.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000L),
          initialValue = VitalisSettings(),
      )

  val recentFoodLog: StateFlow<List<FoodLogEntity>> =
      foodLogRepo
          .observeRecent(limit = 50)
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5_000L),
              initialValue = emptyList(),
          )

  @Volatile private var latestFrame: Bitmap? = null

  private var menuJob: Job? = null
  private var foodLogJob: Job? = null

  private val lastRoastAt = mutableMapOf<String, Long>()
  @Volatile private var pendingMenuPreference: String? = null

  fun updateLatestFrame(bitmap: Bitmap?) {
    latestFrame = bitmap
  }

  // ============================================================
  // Food-logging loop (default behaviour)
  // ============================================================

  fun startFoodLogging() {
    if (foodLogJob?.isActive == true) return
    if (BuildConfig.ANTHROPIC_API_KEY.isBlank()) {
      Log.w(TAG, "Skipping food logging — ANTHROPIC_API_KEY missing")
      return
    }
    val detector = AnthropicFoodDetector(BuildConfig.ANTHROPIC_API_KEY, knownFoods)
    foodLogJob = viewModelScope.launch {
      while (true) {
        delay(FOOD_SAMPLE_INTERVAL_MS)
        if (_uiState.value.phase != AssistantPhase.LOGGING &&
            _uiState.value.phase != AssistantPhase.ERROR) {
          // Paused while menu sub-mode is running.
          continue
        }
        val frame = latestFrame ?: continue
        val snapshot = frame.copy(frame.config ?: Bitmap.Config.ARGB_8888, false)
        try {
          val context = buildPromptContext()
          val detections =
              runCatching { detector.detect(snapshot, context) }
                  .onFailure { Log.e(TAG, "Food detection failed", it) }
                  .getOrDefault(emptyList())
          handleDetections(detections, snapshot, context)
        } finally {
          snapshot.recycle()
        }
      }
    }
  }

  fun stopFoodLogging() {
    foodLogJob?.cancel()
    foodLogJob = null
  }

  private suspend fun handleDetections(
      detections: List<FoodDetection>,
      frame: Bitmap,
      context: PromptContext,
  ) {
    if (detections.isEmpty()) return
    var anyLogged = false
    for (d in detections) {
      val logged = foodLogRepo.tryLog(d)
      if (logged) {
        anyLogged = true
        Log.d(TAG, "Logged: ${d.name} (${d.calories} kcal, junk=${d.isJunk})")
        if (d.isJunk) maybeRoast(d, frame, context)
      }
    }
    if (anyLogged) {
      viewModelScope.launch { audio.playDing() }
    }
  }

  private fun maybeRoast(detection: FoodDetection, frame: Bitmap, context: PromptContext) {
    val key = (detection.label ?: detection.name).lowercase()
    val now = System.currentTimeMillis()
    val last = lastRoastAt[key]
    if (last != null && now - last < ROAST_COOLDOWN_MS) return
    lastRoastAt[key] = now

    val anthropicKey = BuildConfig.ANTHROPIC_API_KEY
    val elevenKey = BuildConfig.ELEVENLABS_API_KEY
    val voiceId = BuildConfig.ELEVENLABS_VOICE_ID
    if (anthropicKey.isBlank() || elevenKey.isBlank() || voiceId.isBlank()) {
      Log.w(TAG, "Skipping roast — one of ANTHROPIC/ELEVENLABS keys is missing")
      return
    }

    val frameCopy = frame.copy(frame.config ?: Bitmap.Config.ARGB_8888, false)
    viewModelScope.launch {
      try {
        val roaster = RoastGenerator(anthropicKey)
        val text =
            roaster.generate(detection.name, context, frameCopy)
                ?: return@launch
        Log.d(TAG, "Roast: $text")
        val tts = ElevenLabsClient(elevenKey, voiceId)
        val bytes = tts.synthesize(text) ?: return@launch
        audio.playTtsBytes(bytes)
      } catch (e: Exception) {
        Log.e(TAG, "Roast pipeline failed", e)
      } finally {
        frameCopy.recycle()
      }
    }
  }

  // ============================================================
  // Menu sub-mode
  // ============================================================

  /** Voice-triggered helper: store the spoken preference, then start scanning. */
  fun startMenuScanningWithPreference(preference: String?, captureStill: suspend () -> Bitmap?) {
    pendingMenuPreference = preference?.takeIf { it.isNotBlank() }
    startMenuScanning(captureStill)
  }

  fun startMenuScanning(captureStill: suspend () -> Bitmap?) {
    if (_uiState.value.phase == AssistantPhase.MENU_SCANNING ||
        _uiState.value.phase == AssistantPhase.MENU_ANALYZING) {
      Log.d(TAG, "startMenuScanning: already running, ignoring")
      return
    }
    val key = BuildConfig.ANTHROPIC_API_KEY
    if (key.isBlank()) {
      _uiState.update {
        it.copy(
            phase = AssistantPhase.ERROR,
            errorMessage =
                "ANTHROPIC_API_KEY is missing from local.properties. Add it and rebuild.",
        )
      }
      return
    }

    _uiState.update {
      it.copy(
          phase = AssistantPhase.MENU_SCANNING,
          statusMessage = "Look at a menu…",
          capturedMenu = null,
          recommendations = emptyList(),
          selectedRecommendationId = null,
          errorMessage = null,
      )
    }

    menuJob?.cancel()
    menuJob = viewModelScope.launch {
      val anthropic = AnthropicClient(key)
      while (_uiState.value.phase == AssistantPhase.MENU_SCANNING) {
        delay(MENU_SAMPLE_INTERVAL_MS)
        if (_uiState.value.phase != AssistantPhase.MENU_SCANNING) break
        val frame = latestFrame
        if (frame == null) {
          Log.d(TAG, "No frame yet, will retry")
          continue
        }
        val snapshot = frame.copy(frame.config ?: Bitmap.Config.ARGB_8888, false)
        val isMenu =
            runCatching { anthropic.isMenu(snapshot) }
                .onFailure { Log.e(TAG, "Menu classifier failed", it) }
                .getOrDefault(false)
        if (!isMenu) {
          snapshot.recycle()
          continue
        }

        _uiState.update { it.copy(statusMessage = "Menu detected — capturing still…") }
        val highRes =
            runCatching { captureStill() }
                .onFailure { Log.e(TAG, "captureStill threw", it) }
                .getOrNull()
        val menuBitmap =
            if (highRes != null) {
              snapshot.recycle()
              highRes
            } else {
              Log.w(TAG, "High-res capture unavailable; falling back to video frame")
              snapshot
            }
        onMenuDetected(menuBitmap)
        return@launch
      }
    }
  }

  fun cancelMenuScanning() {
    menuJob?.cancel()
    menuJob = null
    _uiState.update {
      it.copy(phase = AssistantPhase.LOGGING, statusMessage = null, errorMessage = null)
    }
  }

  private fun onMenuDetected(menuBitmap: Bitmap) {
    _uiState.update {
      it.copy(
          phase = AssistantPhase.MENU_ANALYZING,
          capturedMenu = menuBitmap,
          statusMessage = "Menu detected — reading items…",
      )
    }
    viewModelScope.launch { analyzeMenu(menuBitmap) }
  }

  private suspend fun analyzeMenu(menuBitmap: Bitmap) {
    val ocrLines =
        runCatching { MenuOcr.extract(menuBitmap) }
            .onFailure { Log.e(TAG, "OCR failed", it) }
            .getOrDefault(emptyList())

    if (ocrLines.isEmpty()) {
      _uiState.update {
        it.copy(
            phase = AssistantPhase.ERROR,
            errorMessage = "Couldn't read any text from this menu. Try scanning again.",
        )
      }
      return
    }

    _uiState.update { it.copy(statusMessage = "Asking Sonnet for recommendations…") }

    val anthropic = AnthropicClient(BuildConfig.ANTHROPIC_API_KEY)
    val itemTexts = ocrLines.map { it.text }
    val promptContext = buildPromptContext()
    val recs =
        runCatching {
              anthropic.recommend(
                  menuBitmap = menuBitmap,
                  context = promptContext,
                  knownMenuItems = itemTexts,
                  extraVoicePreference = pendingMenuPreference,
              )
            }
            .onFailure { Log.e(TAG, "Recommendation call failed", it) }
            .getOrDefault(emptyList())
    pendingMenuPreference = null

    if (recs.isEmpty()) {
      _uiState.update {
        it.copy(
            phase = AssistantPhase.ERROR,
            errorMessage = "No recommendations returned. Try again or refine your profile.",
        )
      }
      return
    }

    _uiState.update { it.copy(statusMessage = "Fetching photos…") }

    val pexelsKey = BuildConfig.PEXELS_API_KEY
    val pexels = if (pexelsKey.isNotBlank()) PexelsClient(pexelsKey) else null

    val withBoxes = recs.mapNotNull { rec ->
      val match = matchToOcrLine(rec.itemName, ocrLines) ?: return@mapNotNull null
      MenuRecommendation(
          id = UUID.randomUUID().toString(),
          itemName = rec.itemName,
          reason = rec.reason,
          box = match.box,
          pexelsImageUrl = null,
      )
    }

    if (withBoxes.isEmpty()) {
      _uiState.update {
        it.copy(
            phase = AssistantPhase.ERROR,
            errorMessage = "Recommendations didn't line up with the menu text. Try again.",
        )
      }
      return
    }

    val enriched =
        if (pexels != null) {
          withBoxes
              .map { rec ->
                viewModelScope.async {
                  rec.copy(pexelsImageUrl = pexels.firstImageUrl(rec.itemName))
                }
              }
              .awaitAll()
        } else {
          withBoxes
        }

    _uiState.update {
      it.copy(
          phase = AssistantPhase.MENU_RESULTS,
          recommendations = enriched,
          statusMessage = null,
      )
    }
  }

  private fun matchToOcrLine(itemName: String, ocrLines: List<OcrLine>): OcrLine? {
    val target = itemName.lowercase()
    return ocrLines.firstOrNull { it.text.lowercase() == target }
        ?: ocrLines.firstOrNull { it.text.lowercase().contains(target) }
        ?: ocrLines.firstOrNull { target.contains(it.text.lowercase()) }
  }

  fun selectRecommendation(id: String?) {
    _uiState.update { it.copy(selectedRecommendationId = id) }
  }

  fun resetMenuToLogging() {
    menuJob?.cancel()
    menuJob = null
    _uiState.update {
      it.copy(
          phase = AssistantPhase.LOGGING,
          statusMessage = null,
          capturedMenu = null,
          recommendations = emptyList(),
          selectedRecommendationId = null,
          errorMessage = null,
      )
    }
  }

  fun retryFromError() {
    resetMenuToLogging()
  }

  // ============================================================
  // Restaurant finder (Google Places API New)
  // ============================================================

  fun openRestaurantSearch() {
    val needsPermission = !locationProvider.hasPermission()
    _uiState.update {
      it.copy(
          restaurantSearch =
              RestaurantSearchState(
                  visible = true,
                  phase =
                      if (needsPermission) RestaurantSearchPhase.NEEDS_LOCATION_PERMISSION
                      else RestaurantSearchPhase.IDLE,
              )
      )
    }
  }

  fun closeRestaurantSearch() {
    _uiState.update { it.copy(restaurantSearch = RestaurantSearchState()) }
  }

  fun setRestaurantQuery(query: String) {
    _uiState.update {
      it.copy(restaurantSearch = it.restaurantSearch.copy(query = query))
    }
  }

  /** Call after the user grants location permission via the launcher. */
  fun onLocationPermissionGranted() {
    _uiState.update {
      if (it.restaurantSearch.visible) {
        it.copy(
            restaurantSearch =
                it.restaurantSearch.copy(phase = RestaurantSearchPhase.IDLE, errorMessage = null)
        )
      } else it
    }
  }

  fun onLocationPermissionDenied() {
    _uiState.update {
      it.copy(
          restaurantSearch =
              it.restaurantSearch.copy(
                  phase = RestaurantSearchPhase.NEEDS_LOCATION_PERMISSION,
                  errorMessage = "Location is required to find nearby restaurants.",
              )
      )
    }
  }

  fun runRestaurantSearch() {
    val query = _uiState.value.restaurantSearch.query.trim()
    if (query.isEmpty()) return

    val apiKey = BuildConfig.GOOGLE_PLACES_API_KEY
    if (apiKey.isBlank()) {
      _uiState.update {
        it.copy(
            restaurantSearch =
                it.restaurantSearch.copy(
                    phase = RestaurantSearchPhase.ERROR,
                    errorMessage =
                        "GOOGLE_PLACES_API_KEY is missing from local.properties. Add it and rebuild.",
                )
        )
      }
      return
    }

    if (!locationProvider.hasPermission()) {
      _uiState.update {
        it.copy(
            restaurantSearch =
                it.restaurantSearch.copy(phase = RestaurantSearchPhase.NEEDS_LOCATION_PERMISSION)
        )
      }
      return
    }

    _uiState.update {
      it.copy(
          restaurantSearch =
              it.restaurantSearch.copy(phase = RestaurantSearchPhase.LOADING, errorMessage = null)
      )
    }

    viewModelScope.launch {
      val location = locationProvider.currentLocation()
      if (location == null) {
        _uiState.update {
          it.copy(
              restaurantSearch =
                  it.restaurantSearch.copy(
                      phase = RestaurantSearchPhase.ERROR,
                      errorMessage =
                          "Couldn't get your location. Check that location services are on.",
                  )
          )
        }
        return@launch
      }

      val client = PlacesClient(apiKey)
      val results =
          runCatching {
                client.searchRestaurants(
                    query = query,
                    userLat = location.latitude,
                    userLng = location.longitude,
                )
              }
              .onFailure { Log.e(TAG, "Restaurant search failed", it) }
              .getOrDefault(emptyList())

      if (results.isEmpty()) {
        _uiState.update {
          it.copy(
              restaurantSearch =
                  it.restaurantSearch.copy(
                      phase = RestaurantSearchPhase.ERROR,
                      errorMessage =
                          "No open restaurants matched \"$query\" within 10 km. Try a different search.",
                  )
          )
        }
        return@launch
      }

      _uiState.update {
        it.copy(
            restaurantSearch =
                it.restaurantSearch.copy(
                    phase = RestaurantSearchPhase.RESULTS,
                    results = results,
                    errorMessage = null,
                )
        )
      }
    }
  }

  /** Voice-triggered variant: prefill the query, open the sheet, and run the search immediately. */
  fun runRestaurantSearchWithQuery(query: String) {
    if (query.isBlank()) return
    _uiState.update {
      it.copy(
          restaurantSearch =
              it.restaurantSearch.copy(visible = true, query = query, errorMessage = null)
      )
    }
    runRestaurantSearch()
  }

  // ============================================================
  // Voice assistant
  // ============================================================

  fun startVoiceTurn() {
    viewModelScope.launch {
      val context = buildPromptContext()
      voiceController.startTurn(
          scope = viewModelScope,
          promptContext = context,
          anthropicKey = BuildConfig.ANTHROPIC_API_KEY,
          ttsKey = BuildConfig.ELEVENLABS_API_KEY,
          ttsVoiceId = BuildConfig.ELEVENLABS_VOICE_ID,
      )
    }
  }

  fun cancelVoice() {
    voiceController.cancel()
  }

  fun dismissVoiceOverlay() {
    _uiState.update { it.copy(voice = VoiceState()) }
  }

  override fun onCleared() {
    super.onCleared()
    menuJob?.cancel()
    foodLogJob?.cancel()
    voiceController.cancel()
    captureStillProvider = null
    // Cooldown map lives on this VM-scoped FoodLogRepository instance, so it dies with us.
  }

  class Factory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(AssistantViewModel::class.java))
      @Suppress("UNCHECKED_CAST") return AssistantViewModel(application) as T
    }
  }
}

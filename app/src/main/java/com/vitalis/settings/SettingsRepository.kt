package com.vitalis.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "vitalis_settings")

data class VitalisSettings(
    val personalProfile: String = "",
    val dietaryAvoidance: String = "",
)

class SettingsRepository(private val context: Context) {

  private object Keys {
    val PROFILE = stringPreferencesKey("personal_profile")
    val AVOIDANCE = stringPreferencesKey("dietary_avoidance")
  }

  val settings: Flow<VitalisSettings> =
      context.settingsDataStore.data.map { prefs -> prefs.toSettings() }

  suspend fun update(transform: (VitalisSettings) -> VitalisSettings) {
    context.settingsDataStore.edit { prefs ->
      val next = transform(prefs.toSettings())
      prefs[Keys.PROFILE] = next.personalProfile
      prefs[Keys.AVOIDANCE] = next.dietaryAvoidance
    }
  }

  private fun Preferences.toSettings() =
      VitalisSettings(
          personalProfile = this[Keys.PROFILE].orEmpty(),
          dietaryAvoidance = this[Keys.AVOIDANCE].orEmpty(),
      )
}

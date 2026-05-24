package com.vitalis.profile

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "vitalis_profile")

class ProfileRepository(private val context: Context) {

  private object Keys {
    val NAME = stringPreferencesKey("name")
    val EMAIL = stringPreferencesKey("email")
    val AGE = intPreferencesKey("age")
    val GENDER = stringPreferencesKey("gender")
    val HEIGHT_CM = intPreferencesKey("height_cm")
    val WEIGHT_KG = doublePreferencesKey("weight_kg")
    val PRIMARY_GOAL = stringPreferencesKey("primary_goal")
    val ACTIVITY = stringPreferencesKey("activity")
    val FOCUS = stringSetPreferencesKey("focus_areas")
    val DNA = stringPreferencesKey("dna_notes")
    val NOTES = stringPreferencesKey("free_form_notes")
    val ONBOARD = booleanPreferencesKey("onboarding_complete")
  }

  val profile: Flow<UserProfile> =
      context.profileDataStore.data.map { p -> p.toProfile() }

  suspend fun update(transform: (UserProfile) -> UserProfile) {
    context.profileDataStore.edit { p ->
      val next = transform(p.toProfile())
      p[Keys.NAME] = next.name
      p[Keys.EMAIL] = next.email
      next.age?.let { p[Keys.AGE] = it } ?: p.remove(Keys.AGE)
      next.gender?.let { p[Keys.GENDER] = it.name } ?: p.remove(Keys.GENDER)
      next.heightCm?.let { p[Keys.HEIGHT_CM] = it } ?: p.remove(Keys.HEIGHT_CM)
      next.weightKg?.let { p[Keys.WEIGHT_KG] = it } ?: p.remove(Keys.WEIGHT_KG)
      next.primaryGoal?.let { p[Keys.PRIMARY_GOAL] = it.name } ?: p.remove(Keys.PRIMARY_GOAL)
      next.activityLevel?.let { p[Keys.ACTIVITY] = it.name } ?: p.remove(Keys.ACTIVITY)
      p[Keys.FOCUS] = next.focusAreas.map { it.name }.toSet()
      p[Keys.DNA] = next.dnaNotes
      p[Keys.NOTES] = next.freeFormNotes
      p[Keys.ONBOARD] = next.onboardingComplete
    }
  }

  suspend fun reset() {
    context.profileDataStore.edit { it.clear() }
  }

  private fun Preferences.toProfile(): UserProfile =
      UserProfile(
          name = this[Keys.NAME].orEmpty(),
          email = this[Keys.EMAIL].orEmpty(),
          age = this[Keys.AGE],
          gender = this[Keys.GENDER]?.let { runCatching { Gender.valueOf(it) }.getOrNull() },
          heightCm = this[Keys.HEIGHT_CM],
          weightKg = this[Keys.WEIGHT_KG],
          primaryGoal = this[Keys.PRIMARY_GOAL]?.let { runCatching { PrimaryGoal.valueOf(it) }.getOrNull() },
          activityLevel = this[Keys.ACTIVITY]?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() },
          focusAreas = (this[Keys.FOCUS] ?: emptySet()).mapNotNull { runCatching { FocusArea.valueOf(it) }.getOrNull() }.toSet(),
          dnaNotes = this[Keys.DNA].orEmpty(),
          freeFormNotes = this[Keys.NOTES].orEmpty(),
          onboardingComplete = this[Keys.ONBOARD] ?: false,
      )

  companion object {
    @Volatile private var instance: ProfileRepository? = null
    fun get(context: Context): ProfileRepository =
        instance ?: synchronized(this) {
          instance ?: ProfileRepository(context.applicationContext).also { instance = it }
        }
  }
}

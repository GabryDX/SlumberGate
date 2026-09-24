package com.heronikostudios.slumbergate.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.heronikostudios.slumbergate.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "slumbergate_settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val BEDTIME_HOUR = intPreferencesKey("bedtime_hour")
        val BEDTIME_MINUTE = intPreferencesKey("bedtime_minute")
        val WAKE_HOUR = intPreferencesKey("wake_hour")
        val WAKE_MINUTE = intPreferencesKey("wake_minute")
        val HOME_WIFI_SSID = stringPreferencesKey("home_wifi_ssid")
        val IS_ACTIVE = booleanPreferencesKey("is_active")
        val WHITELISTED_PACKAGES = stringSetPreferencesKey("whitelisted_packages")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_BYPASS_TIMESTAMP = longPreferencesKey("last_bypass_timestamp")
        val LAST_COMPLETED_DATE = stringPreferencesKey("last_completed_date")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferencesToUserSettings(preferences)
        }

    suspend fun getSettingsSnapshot(): UserSettings {
        val preferences = context.dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .first()
        return mapPreferencesToUserSettings(preferences)
    }

    fun getSettingsSnapshotBlocking(): UserSettings {
        return kotlinx.coroutines.runBlocking {
            getSettingsSnapshot()
        }
    }

    private fun mapPreferencesToUserSettings(preferences: Preferences): UserSettings {
        return UserSettings(
            bedtimeHour = preferences[PreferencesKeys.BEDTIME_HOUR] ?: 23,
            bedtimeMinute = preferences[PreferencesKeys.BEDTIME_MINUTE] ?: 0,
            wakeHour = preferences[PreferencesKeys.WAKE_HOUR] ?: 7,
            wakeMinute = preferences[PreferencesKeys.WAKE_MINUTE] ?: 0,
            homeWifiSsid = preferences[PreferencesKeys.HOME_WIFI_SSID] ?: "",
            isActive = preferences[PreferencesKeys.IS_ACTIVE] ?: true,
            whitelistedPackages = preferences[PreferencesKeys.WHITELISTED_PACKAGES] ?: emptySet(),
            streakDays = preferences[PreferencesKeys.STREAK_DAYS] ?: 0,
            lastBypassTimestamp = preferences[PreferencesKeys.LAST_BYPASS_TIMESTAMP] ?: 0L,
            lastCompletedDate = preferences[PreferencesKeys.LAST_COMPLETED_DATE] ?: "",
            isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun updateBedtime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BEDTIME_HOUR] = hour
            preferences[PreferencesKeys.BEDTIME_MINUTE] = minute
        }
    }

    suspend fun updateWakeTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WAKE_HOUR] = hour
            preferences[PreferencesKeys.WAKE_MINUTE] = minute
        }
    }

    suspend fun updateHomeWifiSsid(ssid: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HOME_WIFI_SSID] = ssid
        }
    }

    suspend fun setIsActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ACTIVE] = active
        }
    }

    suspend fun updateWhitelistedPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WHITELISTED_PACKAGES] = packages.take(3).toSet()
        }
    }

    suspend fun recordBypass() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BYPASS_TIMESTAMP] = System.currentTimeMillis()
            preferences[PreferencesKeys.STREAK_DAYS] = 0
        }
    }

    suspend fun completeLockdownNight(dateString: String) {
        context.dataStore.edit { preferences ->
            val lastDate = preferences[PreferencesKeys.LAST_COMPLETED_DATE] ?: ""
            if (lastDate != dateString) {
                val currentStreak = preferences[PreferencesKeys.STREAK_DAYS] ?: 0
                preferences[PreferencesKeys.STREAK_DAYS] = currentStreak + 1
                preferences[PreferencesKeys.LAST_COMPLETED_DATE] = dateString
            }
        }
    }

    suspend fun resetStreak() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STREAK_DAYS] = 0
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }
}

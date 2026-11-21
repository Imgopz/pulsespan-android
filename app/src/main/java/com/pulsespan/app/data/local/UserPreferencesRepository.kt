package com.pulsespan.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pulsespan_prefs")

data class UserPreferences(
    val demoModeEnabled: Boolean = true,
    val targetSleepMinutes: Int = 480,
    val aiCoachingEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DEMO_MODE = booleanPreferencesKey("demo_mode_enabled")
        val TARGET_SLEEP_MIN = intPreferencesKey("target_sleep_minutes")
        val AI_COACHING_ENABLED = booleanPreferencesKey("ai_coaching_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            demoModeEnabled = prefs[Keys.DEMO_MODE] ?: true,
            targetSleepMinutes = prefs[Keys.TARGET_SLEEP_MIN] ?: 480,
            aiCoachingEnabled = prefs[Keys.AI_COACHING_ENABLED] ?: true
        )
    }

    suspend fun setDemoMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEMO_MODE] = enabled }
    }

    suspend fun setTargetSleepMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.TARGET_SLEEP_MIN] = minutes.coerceIn(360, 600) }
    }

    suspend fun setAiCoachingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AI_COACHING_ENABLED] = enabled }
    }
}

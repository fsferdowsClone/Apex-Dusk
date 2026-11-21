package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.game.model.BikeLivery
import com.example.game.model.DailyMissionsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.apexDuskDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "apex_dusk_prefs"
)

data class PersistedSettings(
    val highScore: Int = 0,
    val livery: BikeLivery = BikeLivery.SOLAR_EMBER,
    val isMuted: Boolean = false,
    val dailyMissions: DailyMissionsState = DailyMissionsState(),
)

class PlayerPreferencesRepository(private val context: Context) {

    private object Keys {
        val HIGH_SCORE = intPreferencesKey("high_score")
        val SELECTED_LIVERY = stringPreferencesKey("selected_livery")
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val MISSION_DATE = stringPreferencesKey("daily_mission_date")
        val MISSION_NEAR_MISSES = intPreferencesKey("daily_near_misses")
        val MISSION_DISTANCE_M = intPreferencesKey("daily_distance_meters")
        val MISSION_BEST_SPEED = intPreferencesKey("daily_best_speed_kmh")
    }

    private fun currentDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val settingsFlow: Flow<PersistedSettings> = context.apexDuskDataStore.data.map { prefs ->
        val today = currentDateKey()
        val storedDate = prefs[Keys.MISSION_DATE]
        val missions = if (storedDate == today) {
            DailyMissionsState(
                dateKey = today,
                totalNearMisses = prefs[Keys.MISSION_NEAR_MISSES] ?: 0,
                totalDistanceMeters = prefs[Keys.MISSION_DISTANCE_M] ?: 0,
                bestSpeedKmh = prefs[Keys.MISSION_BEST_SPEED] ?: 0,
            )
        } else {
            DailyMissionsState(dateKey = today)
        }

        PersistedSettings(
            highScore = prefs[Keys.HIGH_SCORE] ?: 0,
            livery = BikeLivery.fromId(prefs[Keys.SELECTED_LIVERY] ?: BikeLivery.SOLAR_EMBER.id),
            isMuted = prefs[Keys.IS_MUTED] ?: false,
            dailyMissions = missions,
        )
    }

    suspend fun saveHighScoreIfHigher(score: Int) {
        context.apexDuskDataStore.edit { prefs ->
            val current = prefs[Keys.HIGH_SCORE] ?: 0
            if (score > current) {
                prefs[Keys.HIGH_SCORE] = score
            }
        }
    }

    suspend fun saveSelectedLivery(livery: BikeLivery) {
        context.apexDuskDataStore.edit { prefs ->
            prefs[Keys.SELECTED_LIVERY] = livery.id
        }
    }

    suspend fun saveMuted(muted: Boolean) {
        context.apexDuskDataStore.edit { prefs ->
            prefs[Keys.IS_MUTED] = muted
        }
    }

    suspend fun saveDailyMissions(missions: DailyMissionsState) {
        val today = currentDateKey()
        context.apexDuskDataStore.edit { prefs ->
            val sameDay = prefs[Keys.MISSION_DATE] == today
            prefs[Keys.MISSION_DATE] = today
            val prevNear = if (sameDay) prefs[Keys.MISSION_NEAR_MISSES] ?: 0 else 0
            val prevDist = if (sameDay) prefs[Keys.MISSION_DISTANCE_M] ?: 0 else 0
            val prevSpeed = if (sameDay) prefs[Keys.MISSION_BEST_SPEED] ?: 0 else 0

            prefs[Keys.MISSION_NEAR_MISSES] = maxOf(prevNear, missions.totalNearMisses)
            prefs[Keys.MISSION_DISTANCE_M] = maxOf(prevDist, missions.totalDistanceMeters)
            prefs[Keys.MISSION_BEST_SPEED] = maxOf(prevSpeed, missions.bestSpeedKmh)
        }
    }
}

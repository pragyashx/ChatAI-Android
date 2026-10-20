package com.chatai.app.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

private val Context.usageLimitDataStore by preferencesDataStore(name = "usage_limits")

data class DailyUsage(
    val used: Int,
    val limit: Int = DailyUsagePolicy.DAILY_LIMIT
) {
    val remaining: Int get() = (limit - used).coerceAtLeast(0)
    val canConsume: Boolean get() = remaining > 0
}

object DailyUsagePolicy {
    const val DAILY_LIMIT = 10
    const val MAX_INPUT_CHARACTERS = 8_000

    fun effectiveCount(storedDay: Long?, storedCount: Int, today: Long): Int =
        if (storedDay == today) storedCount.coerceAtLeast(0) else 0
}

class UsageLimitRepository(private val context: Context) {
    private val dayKey = longPreferencesKey("calendar_day")
    private val countKey = intPreferencesKey("ai_request_count")

    private val currentDay: Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            emit(today())
            delay(DAY_REFRESH_INTERVAL_MS)
        }
    }

    val usage: Flow<DailyUsage> = combine(
        context.usageLimitDataStore.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        },
        currentDay
    ) { preferences, day ->
        DailyUsage(
            used = DailyUsagePolicy.effectiveCount(
                storedDay = preferences[dayKey],
                storedCount = preferences[countKey] ?: 0,
                today = day
            )
        )
    }

    suspend fun tryConsume(): Boolean {
        var consumed = false
        context.usageLimitDataStore.edit { preferences ->
            val day = today()
            val count = DailyUsagePolicy.effectiveCount(
                storedDay = preferences[dayKey],
                storedCount = preferences[countKey] ?: 0,
                today = day
            )
            if (count < DailyUsagePolicy.DAILY_LIMIT) {
                preferences[dayKey] = day
                preferences[countKey] = count + 1
                consumed = true
            }
        }
        return consumed
    }

    private fun today(): Long = LocalDate.now().toEpochDay()

    private companion object {
        const val DAY_REFRESH_INTERVAL_MS = 60_000L
    }
}

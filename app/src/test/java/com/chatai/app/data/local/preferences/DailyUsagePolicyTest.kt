package com.chatai.app.data.local.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyUsagePolicyTest {
    @Test
    fun effectiveCount_keepsTodaysCount() {
        assertEquals(7, DailyUsagePolicy.effectiveCount(100, 7, 100))
    }

    @Test
    fun effectiveCount_resetsOldDay() {
        assertEquals(0, DailyUsagePolicy.effectiveCount(99, 10, 100))
    }

    @Test
    fun dailyUsage_neverReturnsNegativeRemaining() {
        assertEquals(0, DailyUsage(used = 12).remaining)
    }
}

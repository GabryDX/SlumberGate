package com.heronikostudios.slumbergate

import com.heronikostudios.slumbergate.domain.SleepMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SleepMathTest {

    @Test
    fun projectedSleep_sameDayOrNextDay_calculatesAccurately() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val result = SleepMath.calculateProjectedSleep(
            wakeHour = 7,
            wakeMinute = 0,
            now = now
        )

        assertEquals("If you sleep now: 7h 30m of rest.", result)
    }

    @Test
    fun isWithinTimeWindow_crossesMidnight_correctlyIdentifiesInside() {
        // Window 23:00 to 07:00
        val insideLateNight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }
        val insideEarlyMorning = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 15)
        }
        val outsideDaytime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }

        assertTrue(SleepMath.isWithinTimeWindow(23, 0, 7, 0, insideLateNight))
        assertTrue(SleepMath.isWithinTimeWindow(23, 0, 7, 0, insideEarlyMorning))
        assertFalse(SleepMath.isWithinTimeWindow(23, 0, 7, 0, outsideDaytime))
    }

    @Test
    fun isWithinTimeWindow_sameDayWindow_correctlyIdentifies() {
        // Window 01:00 to 08:00
        val inside = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
        }
        val outside = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }

        assertTrue(SleepMath.isWithinTimeWindow(1, 0, 8, 0, inside))
        assertFalse(SleepMath.isWithinTimeWindow(1, 0, 8, 0, outside))
    }
}

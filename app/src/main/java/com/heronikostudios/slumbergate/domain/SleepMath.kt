package com.heronikostudios.slumbergate.domain

import java.util.Calendar

object SleepMath {

    fun calculateProjectedSleep(
        wakeHour: Int,
        wakeMinute: Int,
        now: Calendar = Calendar.getInstance()
    ): String {
        val target = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, wakeHour)
            set(Calendar.MINUTE, wakeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target wake time is earlier or equal to now, it is for tomorrow morning
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val diffMillis = target.timeInMillis - now.timeInMillis
        val diffMinutes = (diffMillis / (1000 * 60))
        val hours = diffMinutes / 60
        val minutes = diffMinutes % 60

        return if (hours > 0) {
            "If you sleep now: ${hours}h ${minutes}m of rest."
        } else {
            "If you sleep now: ${minutes}m of rest."
        }
    }

    fun isWithinTimeWindow(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        now: Calendar = Calendar.getInstance()
    ): Boolean {
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes < endMinutes) {
            // e.g. 01:00 to 07:00 (same day)
            currentMinutes in startMinutes until endMinutes
        } else if (startMinutes > endMinutes) {
            // e.g. 23:00 to 07:00 (crosses midnight)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            // start == end, 24h window
            true
        }
    }

    fun getNextAlarmTime(hour: Int, minute: Int, subtractMinutes: Int = 0): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (subtractMinutes != 0) {
                add(Calendar.MINUTE, -subtractMinutes)
            }
        }

        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }
}

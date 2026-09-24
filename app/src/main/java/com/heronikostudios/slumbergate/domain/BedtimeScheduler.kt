package com.heronikostudios.slumbergate.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.heronikostudios.slumbergate.core.receiver.AlarmReceiver
import com.heronikostudios.slumbergate.data.datastore.SettingsDataStore
import com.heronikostudios.slumbergate.data.model.UserSettings

class BedtimeScheduler(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleBedtimeAlarms(settings: UserSettings) {
        cancelAlarms()

        if (!settings.isActive) {
            return
        }

        // 1. Wind-down alarm (5 minutes before bedtime)
        val windDownTime = SleepMath.getNextAlarmTime(
            settings.bedtimeHour,
            settings.bedtimeMinute,
            subtractMinutes = 5
        )
        setExactAlarm(
            triggerAtMillis = windDownTime,
            action = AlarmReceiver.ACTION_WIND_DOWN_ALARM,
            requestCode = REQUEST_CODE_WIND_DOWN
        )

        // 2. Bedtime alarm (Exact bedtime)
        val bedtimeTime = SleepMath.getNextAlarmTime(
            settings.bedtimeHour,
            settings.bedtimeMinute
        )
        setExactAlarm(
            triggerAtMillis = bedtimeTime,
            action = AlarmReceiver.ACTION_BEDTIME_ALARM,
            requestCode = REQUEST_CODE_BEDTIME
        )

        // 3. Wake-up alarm (Morning wake-up time)
        val wakeTime = SleepMath.getNextAlarmTime(
            settings.wakeHour,
            settings.wakeMinute
        )
        setExactAlarm(
            triggerAtMillis = wakeTime,
            action = AlarmReceiver.ACTION_WAKE_UP_ALARM,
            requestCode = REQUEST_CODE_WAKE
        )
    }

    private fun setExactAlarm(triggerAtMillis: Long, action: String, requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarms() {
        val actions = listOf(
            AlarmReceiver.ACTION_WIND_DOWN_ALARM to REQUEST_CODE_WIND_DOWN,
            AlarmReceiver.ACTION_BEDTIME_ALARM to REQUEST_CODE_BEDTIME,
            AlarmReceiver.ACTION_WAKE_UP_ALARM to REQUEST_CODE_WAKE
        )

        for ((action, code) in actions) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                this.action = action
            }
            val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, code, intent, flags)
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_WIND_DOWN = 1001
        private const val REQUEST_CODE_BEDTIME = 1002
        private const val REQUEST_CODE_WAKE = 1003
    }
}

package com.heronikostudios.slumbergate.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.heronikostudios.slumbergate.SlumberGateApp
import com.heronikostudios.slumbergate.core.service.SleepWatcherService
import com.heronikostudios.slumbergate.data.model.LocationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as SlumberGateApp

        CoroutineScope(Dispatchers.IO).launch {
            val settings = app.settingsDataStore.getSettingsSnapshot()
            if (!settings.isActive) return@launch

            when (action) {
                ACTION_WIND_DOWN_ALARM -> {
                    val location = app.wifiContextManager.evaluateLocation(settings.homeWifiSsid)
                    if (location == LocationState.HOME) {
                        startService(context, SleepWatcherService.ACTION_START_WIND_DOWN)
                    } else {
                        // User is away: defer check by 30 mins
                        app.wifiContextManager.scheduleDeferredCheck(30L)
                    }
                }

                ACTION_BEDTIME_ALARM -> {
                    val location = app.wifiContextManager.evaluateLocation(settings.homeWifiSsid)
                    if (location == LocationState.HOME) {
                        startService(context, SleepWatcherService.ACTION_START_LOCKDOWN)
                    } else {
                        app.wifiContextManager.scheduleDeferredCheck(30L)
                    }
                }

                ACTION_WAKE_UP_ALARM -> {
                    startService(context, SleepWatcherService.ACTION_WAKE_UP)
                }
            }
        }
    }

    private fun startService(context: Context, action: String) {
        val serviceIntent = Intent(context, SleepWatcherService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val ACTION_WIND_DOWN_ALARM = "com.heronikostudios.slumbergate.alarm.WIND_DOWN"
        const val ACTION_BEDTIME_ALARM = "com.heronikostudios.slumbergate.alarm.BEDTIME"
        const val ACTION_WAKE_UP_ALARM = "com.heronikostudios.slumbergate.alarm.WAKE_UP"
    }
}

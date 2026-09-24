package com.heronikostudios.slumbergate.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.heronikostudios.slumbergate.SlumberGateApp
import com.heronikostudios.slumbergate.core.service.SleepWatcherService
import com.heronikostudios.slumbergate.data.model.LocationState
import com.heronikostudios.slumbergate.domain.EvaluateLockConditionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as SlumberGateApp

        CoroutineScope(Dispatchers.IO).launch {
            val settings = app.settingsDataStore.getSettingsSnapshot()
            if (!settings.isActive) return@launch

            val evaluator = EvaluateLockConditionUseCase(app.wifiContextManager)
            val isInSleepWindow = evaluator.isCurrentlyInSleepWindow(
                settings.bedtimeHour,
                settings.bedtimeMinute,
                settings.wakeHour,
                settings.wakeMinute
            )

            if (isInSleepWindow) {
                val location = app.wifiContextManager.evaluateLocation(settings.homeWifiSsid)
                if (location == LocationState.HOME) {
                    val serviceIntent = Intent(context, SleepWatcherService::class.java).apply {
                        action = SleepWatcherService.ACTION_START_LOCKDOWN
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else {
                    app.wifiContextManager.scheduleDeferredCheck(30L)
                }
            } else {
                // Reschedule bedtime alarms for the upcoming bedtime
                app.bedtimeScheduler.scheduleBedtimeAlarms(settings)
            }
        }
    }
}

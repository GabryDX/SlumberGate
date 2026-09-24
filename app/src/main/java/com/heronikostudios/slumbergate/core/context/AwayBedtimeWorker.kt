package com.heronikostudios.slumbergate.core.context

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.heronikostudios.slumbergate.SlumberGateApp
import com.heronikostudios.slumbergate.core.service.SleepWatcherService
import com.heronikostudios.slumbergate.data.model.LocationState
import com.heronikostudios.slumbergate.domain.EvaluateLockConditionUseCase

class AwayBedtimeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SlumberGateApp
        val settings = app.settingsDataStore.getSettingsSnapshot()

        if (!settings.isActive) {
            return Result.success()
        }

        val evaluator = EvaluateLockConditionUseCase(app.wifiContextManager)
        val shouldLock = evaluator.isCurrentlyInSleepWindow(
            settings.bedtimeHour,
            settings.bedtimeMinute,
            settings.wakeHour,
            settings.wakeMinute
        )

        if (!shouldLock) {
            return Result.success()
        }

        val location = app.wifiContextManager.evaluateLocation(settings.homeWifiSsid)
        if (location == LocationState.HOME) {
            val serviceIntent = Intent(applicationContext, SleepWatcherService::class.java).apply {
                action = SleepWatcherService.ACTION_START_LOCKDOWN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)
            } else {
                applicationContext.startService(serviceIntent)
            }
        } else {
            // Still away: schedule another 30 min check
            app.wifiContextManager.scheduleDeferredCheck(30L)
        }

        return Result.success()
    }
}

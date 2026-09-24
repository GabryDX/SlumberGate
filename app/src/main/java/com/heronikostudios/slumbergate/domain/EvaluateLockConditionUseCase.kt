package com.heronikostudios.slumbergate.domain

import com.heronikostudios.slumbergate.core.context.WifiContextManager
import com.heronikostudios.slumbergate.data.model.LocationState
import com.heronikostudios.slumbergate.data.model.UserSettings
import java.util.Calendar

class EvaluateLockConditionUseCase(
    private val wifiContextManager: WifiContextManager
) {
    sealed interface EvaluationResult {
        data object Disabled : EvaluationResult
        data object OutsideWindow : EvaluationResult
        data object AwayDeferred : EvaluationResult
        data class TriggerWindDown(val remainingSeconds: Long) : EvaluationResult
        data object TriggerLockdown : EvaluationResult
    }

    fun isCurrentlyInSleepWindow(
        bedtimeHour: Int,
        bedtimeMinute: Int,
        wakeHour: Int,
        wakeMinute: Int,
        now: Calendar = Calendar.getInstance()
    ): Boolean {
        return SleepMath.isWithinTimeWindow(
            startHour = bedtimeHour,
            startMinute = bedtimeMinute,
            endHour = wakeHour,
            endMinute = wakeMinute,
            now = now
        )
    }

    fun evaluate(settings: UserSettings, now: Calendar = Calendar.getInstance()): EvaluationResult {
        if (!settings.isActive) {
            return EvaluationResult.Disabled
        }

        val inLockdownWindow = isCurrentlyInSleepWindow(
            settings.bedtimeHour,
            settings.bedtimeMinute,
            settings.wakeHour,
            settings.wakeMinute,
            now
        )

        if (inLockdownWindow) {
            val location = wifiContextManager.evaluateLocation(settings.homeWifiSsid)
            return if (location == LocationState.HOME) {
                EvaluationResult.TriggerLockdown
            } else {
                wifiContextManager.scheduleDeferredCheck(30L)
                EvaluationResult.AwayDeferred
            }
        }

        // Check if we are in the 5-minute wind-down runway
        // (bedtime - 5 minutes) up to bedtime
        val windDownStartCal = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, settings.bedtimeHour)
            set(Calendar.MINUTE, settings.bedtimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -5)
        }

        val bedtimeCal = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, settings.bedtimeHour)
            set(Calendar.MINUTE, settings.bedtimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Adjust for day boundaries
        if (bedtimeCal.timeInMillis < windDownStartCal.timeInMillis) {
            bedtimeCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val nowMillis = now.timeInMillis
        if (nowMillis in windDownStartCal.timeInMillis until bedtimeCal.timeInMillis) {
            val location = wifiContextManager.evaluateLocation(settings.homeWifiSsid)
            return if (location == LocationState.HOME) {
                val remainingSeconds = (bedtimeCal.timeInMillis - nowMillis) / 1000
                EvaluationResult.TriggerWindDown(remainingSeconds.coerceIn(0L, 300L))
            } else {
                EvaluationResult.AwayDeferred
            }
        }

        return EvaluationResult.OutsideWindow
    }
}

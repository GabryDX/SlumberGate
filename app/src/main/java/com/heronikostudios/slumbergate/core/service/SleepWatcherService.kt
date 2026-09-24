package com.heronikostudios.slumbergate.core.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.heronikostudios.slumbergate.MainActivity
import com.heronikostudios.slumbergate.R
import com.heronikostudios.slumbergate.SlumberGateApp
import com.heronikostudios.slumbergate.data.model.LockdownState
import com.heronikostudios.slumbergate.data.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SleepWatcherService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    private lateinit var telephonyManager: TelephonyManager
    private var telephonyCallback: Any? = null

    private val _countdownFlow = MutableStateFlow(0L)
    val countdownFlow = _countdownFlow.asStateFlow()

    private val app by lazy { applicationContext as SlumberGateApp }

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        setupTelephonyListener()

        app.overlayManager.onEmergencyUnlockGranted = {
            handleEmergencyUnlock()
        }

        app.overlayManager.onRelockRequested = {
            startLockdown()
        }

        app.flipSensorDetector.onSleepStanceChanged = { isFaceDown ->
            app.overlayManager.setScreenTurnedOff(isFaceDown)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_LOCKDOWN

        startInForeground("SlumberGate is protecting your sleep schedule.")

        serviceScope.launch {
            val settings = app.settingsDataStore.getSettingsSnapshot()
            LockdownStateHolder.whitelistedPackages = settings.whitelistedPackages

            when (action) {
                ACTION_START_WIND_DOWN -> {
                    val duration = intent?.getLongExtra(EXTRA_REMAINING_SECONDS, 300L) ?: 300L
                    startWindDown(duration, settings)
                }

                ACTION_START_LOCKDOWN -> {
                    startLockdown(settings)
                }

                ACTION_EMERGENCY_UNLOCK -> {
                    handleEmergencyUnlock()
                }

                ACTION_WAKE_UP -> {
                    handleWakeUp(settings)
                }

                ACTION_STOP_SERVICE -> {
                    stopService()
                }
            }
        }

        return START_STICKY
    }

    private fun startInForeground(contentMessage: String) {
        val notification = createServiceNotification(contentMessage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startWindDown(durationSeconds: Long, settings: UserSettings) {
        timerJob?.cancel()
        app.overlayManager.hideAll()
        app.flipSensorDetector.stopListening()

        app.overlayManager.showWindDownBanner(durationSeconds)

        timerJob = serviceScope.launch {
            var remaining = durationSeconds
            while (remaining > 0) {
                LockdownStateHolder.updateState(LockdownState.WindDown(remaining))
                _countdownFlow.value = remaining
                app.overlayManager.updateWindDownSeconds(remaining)

                val mins = remaining / 60
                val secs = remaining % 60
                updateNotification(String.format("Preparing for scheduled wind-down. %d:%02d remaining.", mins, secs))

                delay(1000L)
                remaining -= 1
            }

            // Wind-down finished: activate lockdown
            startLockdown(settings)
        }
    }

    private fun startLockdown(settings: UserSettings? = null) {
        timerJob?.cancel()
        app.overlayManager.hideAll()

        serviceScope.launch {
            val userSettings = settings ?: app.settingsDataStore.getSettingsSnapshot()
            LockdownStateHolder.updateState(LockdownState.LockdownActive)

            updateNotification("SlumberGate Lockdown Active. Sleep well.")
            app.overlayManager.showLockdownOverlay(userSettings.wakeHour, userSettings.wakeMinute)
            app.flipSensorDetector.startListening()
        }
    }

    private fun handleEmergencyUnlock() {
        timerJob?.cancel()
        app.overlayManager.hideLockdownOverlay()
        app.flipSensorDetector.stopListening()

        serviceScope.launch {
            app.settingsDataStore.recordBypass()

            val totalEmergencySeconds = 180L // 3 minutes
            app.overlayManager.showEmergencyBadge(totalEmergencySeconds)

            timerJob = serviceScope.launch {
                var remaining = totalEmergencySeconds
                while (remaining > 0) {
                    LockdownStateHolder.updateState(LockdownState.EmergencyUnlocked(remaining))
                    _countdownFlow.value = remaining
                    app.overlayManager.updateEmergencySeconds(remaining)

                    val mins = remaining / 60
                    val secs = remaining % 60
                    updateNotification(String.format("Emergency session active: %d:%02d remaining.", mins, secs))

                    delay(1000L)
                    remaining -= 1
                }

                // 3 minutes expired: re-assert lockdown!
                app.overlayManager.hideEmergencyBadge()
                startLockdown()
            }
        }
    }

    private fun handleWakeUp(settings: UserSettings) {
        timerJob?.cancel()
        app.overlayManager.hideAll()
        app.flipSensorDetector.stopListening()
        LockdownStateHolder.updateState(LockdownState.Inactive)

        serviceScope.launch {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            app.settingsDataStore.completeLockdownNight(todayDate)
            val updated = app.settingsDataStore.getSettingsSnapshot()

            sendMorningNotification(updated.streakDays)
            app.bedtimeScheduler.scheduleBedtimeAlarms(updated)

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopService() {
        timerJob?.cancel()
        app.overlayManager.hideAll()
        app.flipSensorDetector.stopListening()
        LockdownStateHolder.updateState(LockdownState.Inactive)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createServiceNotification(text))
    }

    private fun createServiceNotification(text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SlumberGateApp.CHANNEL_SLEEP_SERVICE)
            .setContentTitle("SlumberGate")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun sendMorningNotification(streak: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, SlumberGateApp.CHANNEL_ALERTS)
            .setContentTitle("Good Morning! 🌅")
            .setContentText("You stayed off late-night screens. Streak: $streak days!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_MORNING_ID, notification)
    }

    private fun setupTelephonyListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallState(state)
                }
            }
            try {
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
            } catch (_: Exception) {
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallState(state)
                }
            }
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            telephonyCallback = listener
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> {
                LockdownStateHolder.isPhoneCallActive = true
                app.overlayManager.hideLockdownOverlay()
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                LockdownStateHolder.isPhoneCallActive = false
                if (LockdownStateHolder.isLockdownActive && !LockdownStateHolder.isEmergencyUnlocked) {
                    serviceScope.launch {
                        val settings = app.settingsDataStore.getSettingsSnapshot()
                        app.overlayManager.showLockdownOverlay(settings.wakeHour, settings.wakeMinute)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        app.flipSensorDetector.stopListening()
        app.overlayManager.hideAll()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let {
                try {
                    telephonyManager.unregisterTelephonyCallback(it)
                } catch (_: Exception) {
                }
            }
        } else {
            @Suppress("DEPRECATION")
            (telephonyCallback as? PhoneStateListener)?.let {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 2001
        const val NOTIFICATION_MORNING_ID = 2002

        const val ACTION_START_WIND_DOWN = "com.heronikostudios.slumbergate.action.START_WIND_DOWN"
        const val ACTION_START_LOCKDOWN = "com.heronikostudios.slumbergate.action.START_LOCKDOWN"
        const val ACTION_EMERGENCY_UNLOCK = "com.heronikostudios.slumbergate.action.EMERGENCY_UNLOCK"
        const val ACTION_WAKE_UP = "com.heronikostudios.slumbergate.action.WAKE_UP"
        const val ACTION_STOP_SERVICE = "com.heronikostudios.slumbergate.action.STOP_SERVICE"

        const val EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"
    }
}

package com.heronikostudios.slumbergate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.heronikostudios.slumbergate.core.context.WifiContextManager
import com.heronikostudios.slumbergate.core.hardware.FlipSensorDetector
import com.heronikostudios.slumbergate.core.overlay.OverlayManager
import com.heronikostudios.slumbergate.data.datastore.SettingsDataStore
import com.heronikostudios.slumbergate.domain.BedtimeScheduler

class SlumberGateApp : Application() {

    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var wifiContextManager: WifiContextManager
        private set
    lateinit var bedtimeScheduler: BedtimeScheduler
        private set
    lateinit var overlayManager: OverlayManager
        private set
    lateinit var flipSensorDetector: FlipSensorDetector
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsDataStore = SettingsDataStore(applicationContext)
        wifiContextManager = WifiContextManager(applicationContext)
        bedtimeScheduler = BedtimeScheduler(applicationContext, settingsDataStore)
        overlayManager = OverlayManager(applicationContext)
        flipSensorDetector = FlipSensorDetector(applicationContext)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val serviceChannel = NotificationChannel(
                CHANNEL_SLEEP_SERVICE,
                "Sleep Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active status of SlumberGate night lockout"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Wind-Down & Wake Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for morning milestones and wind-down reminders"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_SLEEP_SERVICE = "slumbergate_sleep_service"
        const val CHANNEL_ALERTS = "slumbergate_alerts"

        lateinit var instance: SlumberGateApp
            private set
    }
}

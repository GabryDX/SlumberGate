package com.heronikostudios.slumbergate.core.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityEvent
import com.heronikostudios.slumbergate.SlumberGateApp

class LockdownAccessibilityService : AccessibilityService() {

    private val telecomManager by lazy {
        getSystemService(TELECOM_SERVICE) as? TelecomManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!LockdownStateHolder.isLockdownActive) return
        if (LockdownStateHolder.isPhoneCallActive || LockdownStateHolder.isEmergencyUnlocked) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            if (!isPackageAllowed(packageName)) {
                // Instantly block and redirect to home
                performGlobalAction(GLOBAL_ACTION_HOME)

                // Ensure the overlay remains assertive
                try {
                    val app = application as? SlumberGateApp
                    app?.let {
                        val settings = it.settingsDataStore.getSettingsSnapshotBlocking()
                        it.overlayManager.showLockdownOverlay(settings.wakeHour, settings.wakeMinute)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun isPackageAllowed(packageName: String): Boolean {
        // SlumberGate itself
        if (packageName == applicationContext.packageName) return true

        // User configured emergency whitelist (e.g. Spotify, Calm)
        if (LockdownStateHolder.whitelistedPackages.contains(packageName)) return true

        // System dialer / phone app
        val defaultDialer = telecomManager?.defaultDialerPackage
        if (defaultDialer != null && packageName == defaultDialer) return true
        if (isDialerPackage(packageName)) return true

        // Clock / Alarm apps
        if (isClockPackage(packageName)) return true

        // Android System UI components that shouldn't be blocked (like volume dialogs or input methods)
        if (isSystemInputOrEssential(packageName)) return true

        return false
    }

    private val allowedPackagesCache = mutableSetOf<String>()

    private fun isDialerPackage(packageName: String): Boolean {
        if (allowedPackagesCache.contains(packageName)) return true
        if (packageName.contains("dialer", ignoreCase = true) ||
            packageName.contains("telecom", ignoreCase = true) ||
            packageName.contains("incall", ignoreCase = true)
        ) {
            allowedPackagesCache.add(packageName)
            return true
        }
        val dialIntent = Intent(Intent.ACTION_DIAL)
        val resolved = packageManager.queryIntentActivities(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val matches = resolved.any { it.activityInfo.packageName == packageName }
        if (matches) allowedPackagesCache.add(packageName)
        return matches
    }

    private fun isClockPackage(packageName: String): Boolean {
        if (allowedPackagesCache.contains(packageName)) return true
        if (packageName.contains("clock", ignoreCase = true) ||
            packageName.contains("deskclock", ignoreCase = true)
        ) {
            allowedPackagesCache.add(packageName)
            return true
        }
        val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        val resolved = packageManager.queryIntentActivities(alarmIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val matches = resolved.any { it.activityInfo.packageName == packageName }
        if (matches) allowedPackagesCache.add(packageName)
        return matches
    }

    private fun isSystemInputOrEssential(packageName: String): Boolean {
        return packageName.contains("inputmethod", ignoreCase = true) ||
                packageName.contains("latin", ignoreCase = true) ||
                packageName.contains("gboard", ignoreCase = true)
    }

    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }
}

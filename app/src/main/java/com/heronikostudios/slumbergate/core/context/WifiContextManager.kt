package com.heronikostudios.slumbergate.core.context

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.heronikostudios.slumbergate.data.model.LocationState
import java.util.concurrent.TimeUnit

class WifiContextManager(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun getCurrentSsid(): String? {
        if (!hasLocationPermission()) {
            return null
        }

        var ssid: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val wifiInfo = capabilities.transportInfo as? WifiInfo
                ssid = wifiInfo?.ssid
            }
        }

        // Fallback or earlier API
        if (ssid == null || ssid == WifiManager.UNKNOWN_SSID || ssid == "<unknown ssid>") {
            @Suppress("DEPRECATION")
            val connectionInfo = wifiManager.connectionInfo
            ssid = connectionInfo?.ssid
        }

        return cleanSsid(ssid)
    }

    fun evaluateLocation(homeSsid: String): LocationState {
        // If user has not configured a home Wi-Fi SSID yet, assume HOME to avoid disabling protection by default
        if (homeSsid.isBlank()) {
            return LocationState.HOME
        }

        val currentSsid = getCurrentSsid()
        return if (currentSsid != null && currentSsid.equals(cleanSsid(homeSsid), ignoreCase = true)) {
            LocationState.HOME
        } else {
            LocationState.AWAY
        }
    }

    fun scheduleDeferredCheck(delayMinutes: Long = 30L) {
        val workRequest = OneTimeWorkRequestBuilder<AwayBedtimeWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_DEFERRED_BEDTIME_CHECK,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelDeferredCheck() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_DEFERRED_BEDTIME_CHECK)
    }

    private fun cleanSsid(rawSsid: String?): String? {
        if (rawSsid == null || rawSsid == WifiManager.UNKNOWN_SSID || rawSsid == "<unknown ssid>") {
            return null
        }
        val trimmed = rawSsid.trim()
        return if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            trimmed
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val WORK_DEFERRED_BEDTIME_CHECK = "deferred_bedtime_check"
    }
}

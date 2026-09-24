package com.heronikostudios.slumbergate.presentation.dashboard

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heronikostudios.slumbergate.SlumberGateApp
import com.heronikostudios.slumbergate.core.service.LockdownStateHolder
import com.heronikostudios.slumbergate.core.service.SleepWatcherService
import com.heronikostudios.slumbergate.data.datastore.SettingsDataStore
import com.heronikostudios.slumbergate.data.model.LockdownState
import com.heronikostudios.slumbergate.data.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val settings: UserSettings = UserSettings(),
    val currentConnectedSsid: String? = null,
    val installedApps: List<AppInfo> = emptyList(),
    val activeLockdownState: LockdownState = LockdownState.Inactive
)

class DashboardViewModel(
    private val app: SlumberGateApp
) : ViewModel() {

    private val settingsDataStore: SettingsDataStore = app.settingsDataStore

    val settings: StateFlow<UserSettings> = settingsDataStore.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _currentConnectedSsid = MutableStateFlow<String?>(null)
    val currentConnectedSsid = _currentConnectedSsid.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    val lockdownState: StateFlow<LockdownState> = LockdownStateHolder.lockdownState

    init {
        refreshCurrentSsid()
        loadInstalledApps()
        viewModelScope.launch {
            settings.collect { userSettings ->
                app.bedtimeScheduler.scheduleBedtimeAlarms(userSettings)
            }
        }
    }

    fun refreshCurrentSsid() {
        _currentConnectedSsid.value = app.wifiContextManager.getCurrentSsid()
    }

    fun setBedtime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.updateBedtime(hour, minute)
        }
    }

    fun setWakeTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.updateWakeTime(hour, minute)
        }
    }

    fun setHomeWifiAsCurrent() {
        val currentSsid = app.wifiContextManager.getCurrentSsid()
        if (!currentSsid.isNullOrBlank()) {
            viewModelScope.launch {
                settingsDataStore.updateHomeWifiSsid(currentSsid)
            }
        }
    }

    fun clearHomeWifi() {
        viewModelScope.launch {
            settingsDataStore.updateHomeWifiSsid("")
        }
    }

    fun toggleMasterActive(active: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setIsActive(active)
            if (!active) {
                stopActiveTest()
            }
        }
    }

    fun toggleWhitelistedApp(packageName: String) {
        viewModelScope.launch {
            val currentList = settings.value.whitelistedPackages.toMutableSet()
            if (currentList.contains(packageName)) {
                currentList.remove(packageName)
            } else {
                if (currentList.size < 3) {
                    currentList.add(packageName)
                }
            }
            settingsDataStore.updateWhitelistedPackages(currentList)
        }
    }

    fun startTestLockdown() {
        val serviceIntent = Intent(app, SleepWatcherService::class.java).apply {
            action = SleepWatcherService.ACTION_START_LOCKDOWN
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(serviceIntent)
        } else {
            app.startService(serviceIntent)
        }
    }

    fun startTestWindDown() {
        val serviceIntent = Intent(app, SleepWatcherService::class.java).apply {
            action = SleepWatcherService.ACTION_START_WIND_DOWN
            putExtra(SleepWatcherService.EXTRA_REMAINING_SECONDS, 60L) // 1 minute test runway
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(serviceIntent)
        } else {
            app.startService(serviceIntent)
        }
    }

    fun stopActiveTest() {
        val serviceIntent = Intent(app, SleepWatcherService::class.java).apply {
            action = SleepWatcherService.ACTION_STOP_SERVICE
        }
        app.startService(serviceIntent)
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = app.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfoList = pm.queryIntentActivities(intent, 0)
                resolveInfoList
                    .map { info ->
                        val pkg = info.activityInfo.packageName
                        val name = info.loadLabel(pm).toString()
                        AppInfo(packageName = pkg, appName = name)
                    }
                    .filter { it.packageName != app.packageName }
                    .distinctBy { it.packageName }
                    .sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
        }
    }
}

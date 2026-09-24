package com.heronikostudios.slumbergate.core.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.heronikostudios.slumbergate.presentation.overlay.EmergencyFloatingBadge
import com.heronikostudios.slumbergate.presentation.overlay.SimulatedShutdownOverlay
import com.heronikostudios.slumbergate.presentation.overlay.WindDownBanner

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var lockdownView: ComposeView? = null
    private var lockdownLifecycleOwner: OverlayLifecycleOwner? = null
    private var lockdownParams: WindowManager.LayoutParams? = null

    private var windDownView: ComposeView? = null
    private var windDownLifecycleOwner: OverlayLifecycleOwner? = null

    private var emergencyBadgeView: ComposeView? = null
    private var emergencyBadgeLifecycleOwner: OverlayLifecycleOwner? = null

    // State holders
    private var isScreenTurnedOffState by mutableStateOf(false)
    private var windDownRemainingSecondsState by mutableLongStateOf(300L)
    private var emergencyRemainingSecondsState by mutableLongStateOf(180L)

    var onEmergencyUnlockGranted: (() -> Unit)? = null
    var onRelockRequested: (() -> Unit)? = null

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    // --- Simulated Shutdown Fullscreen Overlay ---

    fun showLockdownOverlay(wakeHour: Int, wakeMinute: Int) {
        if (!canDrawOverlays() || lockdownView != null) return

        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.onCreate()
        lockdownLifecycleOwner = lifecycleOwner

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            screenBrightness = if (isScreenTurnedOffState) 0.0f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        lockdownParams = params

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                SimulatedShutdownOverlay(
                    wakeHour = wakeHour,
                    wakeMinute = wakeMinute,
                    isScreenTurnedOff = isScreenTurnedOffState,
                    onEmergencyUnlockGranted = {
                        onEmergencyUnlockGranted?.invoke()
                    }
                )
            }
        }

        try {
            windowManager.addView(composeView, params)
            lockdownView = composeView
        } catch (_: Exception) {
            lifecycleOwner.onDestroy()
            lockdownLifecycleOwner = null
        }
    }

    fun hideLockdownOverlay() {
        lockdownView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
        lockdownLifecycleOwner?.onDestroy()
        lockdownView = null
        lockdownLifecycleOwner = null
        lockdownParams = null
    }

    fun setScreenTurnedOff(turnedOff: Boolean) {
        isScreenTurnedOffState = turnedOff
        lockdownParams?.let { params ->
            params.screenBrightness = if (turnedOff) 0.0f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            lockdownView?.let { view ->
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {
                }
            }
        }
    }

    // --- Wind-Down Runway Banner ---

    fun showWindDownBanner(initialRemainingSeconds: Long) {
        if (!canDrawOverlays()) return
        windDownRemainingSecondsState = initialRemainingSeconds

        if (windDownView != null) return

        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.onCreate()
        windDownLifecycleOwner = lifecycleOwner

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                WindDownBanner(
                    remainingSeconds = windDownRemainingSecondsState
                )
            }
        }

        try {
            windowManager.addView(composeView, params)
            windDownView = composeView
        } catch (_: Exception) {
            lifecycleOwner.onDestroy()
            windDownLifecycleOwner = null
        }
    }

    fun updateWindDownSeconds(remainingSeconds: Long) {
        windDownRemainingSecondsState = remainingSeconds
    }

    fun hideWindDownBanner() {
        windDownView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
        windDownLifecycleOwner?.onDestroy()
        windDownView = null
        windDownLifecycleOwner = null
    }

    // --- Emergency Floating Badge ---

    fun showEmergencyBadge(initialRemainingSeconds: Long) {
        if (!canDrawOverlays()) return
        emergencyRemainingSecondsState = initialRemainingSeconds

        if (emergencyBadgeView != null) return

        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.onCreate()
        emergencyBadgeLifecycleOwner = lifecycleOwner

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
        }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                EmergencyFloatingBadge(
                    remainingSeconds = emergencyRemainingSecondsState,
                    onRelockClick = {
                        onRelockRequested?.invoke()
                    }
                )
            }
        }

        try {
            windowManager.addView(composeView, params)
            emergencyBadgeView = composeView
        } catch (_: Exception) {
            lifecycleOwner.onDestroy()
            emergencyBadgeLifecycleOwner = null
        }
    }

    fun updateEmergencySeconds(remainingSeconds: Long) {
        emergencyRemainingSecondsState = remainingSeconds
    }

    fun hideEmergencyBadge() {
        emergencyBadgeView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
        emergencyBadgeLifecycleOwner?.onDestroy()
        emergencyBadgeView = null
        emergencyBadgeLifecycleOwner = null
    }

    fun hideAll() {
        hideLockdownOverlay()
        hideWindDownBanner()
        hideEmergencyBadge()
    }
}

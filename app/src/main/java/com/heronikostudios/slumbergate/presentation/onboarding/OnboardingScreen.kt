package com.heronikostudios.slumbergate.presentation.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.heronikostudios.slumbergate.ui.theme.AccentGreen
import com.heronikostudios.slumbergate.ui.theme.AmberPrimary
import com.heronikostudios.slumbergate.ui.theme.DarkBackground
import com.heronikostudios.slumbergate.ui.theme.DarkSurface
import com.heronikostudios.slumbergate.ui.theme.TextMuted

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasOverlay by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionHelper.hasAccessibilityPermission(context)) }
    var hasLocation by remember { mutableStateOf(PermissionHelper.hasLocationPermission(context)) }
    var hasNotification by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var isBatteryExempt by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }

    // Refresh permissions whenever the user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = PermissionHelper.hasOverlayPermission(context)
                hasAccessibility = PermissionHelper.hasAccessibilityPermission(context)
                hasLocation = PermissionHelper.hasLocationPermission(context)
                hasNotification = PermissionHelper.hasNotificationPermission(context)
                isBatteryExempt = PermissionHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocation = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotification = granted
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome to SlumberGate",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "SlumberGate provides high-friction nighttime protection. Enable these system permissions so the app can safeguard your sleep schedule.",
                    color = TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 1. Overlay
            item {
                PermissionCard(
                    icon = Icons.Default.Layers,
                    title = "Simulated Shutdown Overlay",
                    description = "Required to display the AMOLED-black night lock screen over other apps.",
                    isGranted = hasOverlay,
                    onActionClick = {
                        try {
                            context.startActivity(PermissionHelper.getOverlayPermissionIntent(context))
                        } catch (_: Exception) {
                        }
                    }
                )
            }

            // 2. Accessibility
            item {
                PermissionCard(
                    icon = Icons.Default.Security,
                    title = "Anti-Bypass Guard",
                    description = "Needed by the Accessibility Service to block muscle-memory exits during lockdown.",
                    isGranted = hasAccessibility,
                    onActionClick = {
                        try {
                            context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                        } catch (_: Exception) {
                        }
                    }
                )
            }

            // 3. Location / Wi-Fi
            item {
                PermissionCard(
                    icon = Icons.Default.Wifi,
                    title = "Home Wi-Fi Detection",
                    description = "Needed to read your Wi-Fi SSID so you are never locked out when away from home.",
                    isGranted = hasLocation,
                    onActionClick = {
                        locationLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }

            // 4. Notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    PermissionCard(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        description = "Displays the background service status and morning sleep protection streaks.",
                        isGranted = hasNotification,
                        onActionClick = {
                            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
            }

            // 5. Battery Optimization
            item {
                PermissionCard(
                    icon = Icons.Default.BatteryAlert,
                    title = "Battery Exemption",
                    description = "Prevents OEM aggressive memory killers from terminating bedtime timers.",
                    isGranted = isBatteryExempt,
                    onActionClick = {
                        try {
                            context.startActivity(PermissionHelper.getBatteryOptimizationIntent(context))
                        } catch (_: Exception) {
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onFinishOnboarding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (hasOverlay && hasAccessibility) "Continue to Dashboard" else "Continue Anyway",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) AccentGreen.copy(alpha = 0.15f) else AmberPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (isGranted) AccentGreen else AmberPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isGranted) {
                Text(
                    text = "Active",
                    color = AccentGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                OutlinedButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AmberPrimary
                    )
                ) {
                    Text(
                        text = "Enable",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

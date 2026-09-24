package com.heronikostudios.slumbergate.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heronikostudios.slumbergate.data.model.LockdownState
import com.heronikostudios.slumbergate.domain.SleepMath
import com.heronikostudios.slumbergate.ui.theme.AmberDim
import com.heronikostudios.slumbergate.ui.theme.AmberGlow
import com.heronikostudios.slumbergate.ui.theme.AmberPrimary
import com.heronikostudios.slumbergate.ui.theme.DarkBackground
import com.heronikostudios.slumbergate.ui.theme.DarkSurface
import com.heronikostudios.slumbergate.ui.theme.DarkSurfaceVariant
import com.heronikostudios.slumbergate.ui.theme.TextMuted

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val currentSsid by viewModel.currentConnectedSsid.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val lockdownState by viewModel.lockdownState.collectAsState()

    var showBedtimeDialog by remember { mutableStateOf(false) }
    var showWakeDialog by remember { mutableStateOf(false) }
    var showWhitelistSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshCurrentSsid()
    }

    if (showBedtimeDialog) {
        SlumberTimePickerDialog(
            title = "Set Bedtime",
            initialHour = settings.bedtimeHour,
            initialMinute = settings.bedtimeMinute,
            onConfirm = { h, m -> viewModel.setBedtime(h, m) },
            onDismiss = { showBedtimeDialog = false }
        )
    }

    if (showWakeDialog) {
        SlumberTimePickerDialog(
            title = "Set Morning Wake-Up",
            initialHour = settings.wakeHour,
            initialMinute = settings.wakeMinute,
            onConfirm = { h, m -> viewModel.setWakeTime(h, m) },
            onDismiss = { showWakeDialog = false }
        )
    }

    if (showWhitelistSheet) {
        WhitelistBottomSheet(
            installedApps = installedApps,
            selectedPackages = settings.whitelistedPackages,
            onToggleApp = { pkg -> viewModel.toggleWhitelistedApp(pkg) },
            onDismiss = { showWhitelistSheet = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header with Master Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SlumberGate",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (settings.isActive) "Protection Scheduled" else "Protection Paused",
                            color = if (settings.isActive) AmberPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Switch(
                        checked = settings.isActive,
                        onCheckedChange = { viewModel.toggleMasterActive(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AmberPrimary,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF2B2D3D)
                        )
                    )
                }
            }

            // Streak Card
            item {
                StreakCard(streakDays = settings.streakDays)
            }

            // Schedule Card
            item {
                ScheduleCard(
                    bedtimeFormatted = settings.formattedBedtime,
                    wakeFormatted = settings.formattedWakeTime,
                    projectedSleep = SleepMath.calculateProjectedSleep(settings.wakeHour, settings.wakeMinute),
                    onEditBedtime = { showBedtimeDialog = true },
                    onEditWakeTime = { showWakeDialog = true }
                )
            }

            // Home Wi-Fi Automation Card
            item {
                WifiConfigCard(
                    savedHomeSsid = settings.homeWifiSsid,
                    currentSsid = currentSsid,
                    onSetCurrentAsHome = { viewModel.setHomeWifiAsCurrent() },
                    onClearHome = { viewModel.clearHomeWifi() }
                )
            }

            // Whitelist Card
            item {
                WhitelistSummaryCard(
                    whitelistedCount = settings.whitelistedPackages.size,
                    onOpenWhitelist = { showWhitelistSheet = true }
                )
            }

            // Sandbox & Testing Card
            item {
                SandboxTestingCard(
                    lockdownState = lockdownState,
                    onTestLockdown = { viewModel.startTestLockdown() },
                    onTestWindDown = { viewModel.startTestWindDown() },
                    onStopTest = { viewModel.stopActiveTest() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onNavigateToPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextMuted
                    )
                ) {
                    Text("Review System Permissions", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StreakCard(streakDays: Int) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF261805)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = AmberGlow,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$streakDays Day Streak",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Bypasses avoided, sleep protected",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    bedtimeFormatted: String,
    wakeFormatted: String,
    projectedSleep: String,
    onEditBedtime: () -> Unit,
    onEditWakeTime: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Sleep Schedule",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bedtime item
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onEditBedtime() }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Bedtime", color = TextMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = bedtimeFormatted,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Wake-up item
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onEditWakeTime() }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = AmberGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Wake-Up", color = TextMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = wakeFormatted,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = projectedSleep,
                color = AmberPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WifiConfigCard(
    savedHomeSsid: String,
    currentSsid: String?,
    onSetCurrentAsHome: () -> Unit,
    onClearHome: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Home Automation",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (savedHomeSsid.isNotBlank()) {
                    "Configured Home: \"$savedHomeSsid\""
                } else {
                    "No Home Wi-Fi set (Engages anywhere at bedtime)"
                },
                color = if (savedHomeSsid.isNotBlank()) Color.White else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (!currentSsid.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Connected network: \"$currentSsid\"",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSetCurrentAsHome,
                    enabled = !currentSsid.isNullOrBlank() && currentSsid != savedHomeSsid,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPrimary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (!currentSsid.isNullOrBlank()) "Set \"$currentSsid\" as Home" else "Connect Wi-Fi to Set",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (savedHomeSsid.isNotBlank()) {
                    OutlinedButton(
                        onClick = onClearHome,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextMuted
                        )
                    ) {
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WhitelistSummaryCard(
    whitelistedCount: Int,
    onOpenWhitelist: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222433)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Emergency Whitelist",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$whitelistedCount/3 apps allowed during lockdown",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            OutlinedButton(
                onClick = onOpenWhitelist,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AmberPrimary
                )
            ) {
                Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SandboxTestingCard(
    lockdownState: LockdownState,
    onTestLockdown: () -> Unit,
    onTestWindDown: () -> Unit,
    onStopTest: () -> Unit
) {
    val isRunning = lockdownState !is LockdownState.Inactive

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Instant Simulation Sandbox",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Experience the shutdown screen, 60s breathing unlock, and flip-to-sleep sensor without waiting until nighttime.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTestLockdown,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate Lock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onTestWindDown,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AmberPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Wind-Down", fontSize = 12.sp)
                    }
                }
            } else {
                Button(
                    onClick = onStopTest,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exit Simulation", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

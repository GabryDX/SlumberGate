package com.heronikostudios.slumbergate.presentation.overlay

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heronikostudios.slumbergate.domain.SleepMath
import com.heronikostudios.slumbergate.ui.theme.AbsoluteBlack
import com.heronikostudios.slumbergate.ui.theme.AmberDim
import com.heronikostudios.slumbergate.ui.theme.AmberPrimary
import com.heronikostudios.slumbergate.ui.theme.TextDimAmber
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SimulatedShutdownOverlay(
    wakeHour: Int,
    wakeMinute: Int,
    isScreenTurnedOff: Boolean,
    onEmergencyUnlockGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeString by remember { mutableStateOf("") }
    var sleepRestProjection by remember { mutableStateOf("") }
    var isBreathingActive by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(wakeHour, wakeMinute) {
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            sleepRestProjection = SleepMath.calculateProjectedSleep(wakeHour, wakeMinute)
            // Align delay precisely to the next minute boundary to avoid 3,600 needless wakeups/hour
            val currentSeconds = (System.currentTimeMillis() / 1000) % 60
            val secondsToNextMinute = (60 - currentSeconds).coerceAtLeast(1)
            delay(secondsToNextMinute * 1000L + 50L)
        }
    }

    if (isScreenTurnedOff) {
        // Pure unlit black screen for Flip to Sleep
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AbsoluteBlack)
        )
        return
    }

    if (isBreathingActive) {
        BreathingUnlockDialog(
            onUnlockGranted = {
                isBreathingActive = false
                onEmergencyUnlockGranted()
            },
            onCancel = {
                isBreathingActive = false
            }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AbsoluteBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Central Sleep Display
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTimeString,
                color = AmberDim,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = sleepRestProjection,
                color = TextDimAmber,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = AmberDim.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "  Flip face-down to rest",
                    color = AmberDim.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }

        // Minimalist Action Bar at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayActionButton(
                icon = Icons.Default.Phone,
                label = "Dialer",
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(dialIntent)
                }
            )

            OverlayActionButton(
                icon = Icons.Default.Alarm,
                label = "Alarms",
                onClick = {
                    val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (alarmIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(alarmIntent)
                    }
                }
            )

            OverlayActionButton(
                icon = Icons.Default.LockOpen,
                label = "Unlock",
                onClick = {
                    isBreathingActive = true
                }
            )
        }
    }
}

@Composable
private fun OverlayActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF140B00)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AmberDim,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = AmberDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

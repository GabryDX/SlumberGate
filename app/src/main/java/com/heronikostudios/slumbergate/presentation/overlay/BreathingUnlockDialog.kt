package com.heronikostudios.slumbergate.presentation.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heronikostudios.slumbergate.ui.theme.AbsoluteBlack
import com.heronikostudios.slumbergate.ui.theme.AmberDim
import com.heronikostudios.slumbergate.ui.theme.AmberGlow
import com.heronikostudios.slumbergate.ui.theme.AmberPrimary
import com.heronikostudios.slumbergate.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun BreathingUnlockDialog(
    onUnlockGranted: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var secondsLeft by remember { mutableIntStateOf(60) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
        if (secondsLeft == 0) {
            onUnlockGranted()
        }
    }

    val elapsed = 60 - secondsLeft
    val phaseSecond = elapsed % 12
    val (instruction, targetScale) = when (phaseSecond) {
        in 0..3 -> "Inhale slowly..." to 1.0f
        in 4..7 -> "Hold gently..." to 1.0f
        else -> "Exhale calmly..." to 0.5f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
        label = "breathing_circle_anim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AbsoluteBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Is this truly urgent?",
                color = AmberPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Take 60 seconds to breathe.",
                color = TextMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animated Breathing Circle Canvas
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = (size.minDimension / 2f) * animatedScale
                    // Outer subtle glow
                    drawCircle(
                        color = AmberDim.copy(alpha = 0.25f),
                        radius = radius,
                    )
                    // Inner stroke circle
                    drawCircle(
                        color = AmberGlow,
                        radius = radius * 0.95f,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${secondsLeft}s",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = instruction,
                        color = AmberPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextMuted
                )
            ) {
                Text(
                    text = "Back to Sleep",
                    fontSize = 14.sp
                )
            }
        }
    }
}

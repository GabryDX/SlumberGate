package com.heronikostudios.slumbergate.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.heronikostudios.slumbergate.ui.theme.AmberPrimary
import com.heronikostudios.slumbergate.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlumberTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Color(0xFF222433),
                        selectorColor = AmberPrimary,
                        containerColor = DarkSurface,
                        timeSelectorSelectedContainerColor = AmberPrimary,
                        timeSelectorUnselectedContainerColor = Color(0xFF222433),
                        timeSelectorSelectedContentColor = Color.Black,
                        timeSelectorUnselectedContentColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    TextButton(onClick = {
                        onConfirm(state.hour, state.minute)
                        onDismiss()
                    }) {
                        Text("Confirm", color = AmberPrimary)
                    }
                }
            }
        }
    }
}

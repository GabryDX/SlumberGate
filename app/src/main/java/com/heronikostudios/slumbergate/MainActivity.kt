package com.heronikostudios.slumbergate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heronikostudios.slumbergate.presentation.dashboard.DashboardScreen
import com.heronikostudios.slumbergate.presentation.dashboard.DashboardViewModel
import com.heronikostudios.slumbergate.presentation.onboarding.OnboardingScreen
import com.heronikostudios.slumbergate.presentation.onboarding.PermissionHelper
import com.heronikostudios.slumbergate.ui.theme.DarkBackground
import com.heronikostudios.slumbergate.ui.theme.SlumberGateTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SlumberGateApp

        setContent {
            SlumberGateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val settings by app.settingsDataStore.userSettingsFlow.collectAsState(
                        initial = null
                    )

                    val scope = rememberCoroutineScope()
                    var forcedOnboarding by remember { mutableStateOf(false) }

                    val needsOnboarding = settings?.let {
                        !it.isOnboardingCompleted && !PermissionHelper.areEssentialPermissionsGranted(this)
                    } ?: false

                    val showOnboarding = forcedOnboarding || needsOnboarding

                    val viewModel: DashboardViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return DashboardViewModel(app) as T
                            }
                        }
                    )

                    if (showOnboarding) {
                        OnboardingScreen(
                            onFinishOnboarding = {
                                scope.launch {
                                    app.settingsDataStore.setOnboardingCompleted(true)
                                }
                                forcedOnboarding = false
                            }
                        )
                    } else {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToPermissions = {
                                forcedOnboarding = true
                            }
                        )
                    }
                }
            }
        }
    }
}
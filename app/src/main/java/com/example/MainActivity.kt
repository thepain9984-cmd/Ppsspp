package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.GamepadRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PlaygroundScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.SetupGuideScreen
import com.example.ui.theme.CustomControlsTheme
import com.example.ui.theme.MidnightBackground

enum class ScreenState {
    DASHBOARD,
    PLAYGROUND,
    PROFILES,
    GUIDE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = GamepadRepository.getInstance(applicationContext)

        setContent {
            CustomControlsTheme {
                var currentScreen by remember { mutableStateOf(ScreenState.DASHBOARD) }

                BackHandler(enabled = currentScreen != ScreenState.DASHBOARD) {
                    currentScreen = ScreenState.DASHBOARD
                }

                Scaffold(
                    containerColor = MidnightBackground,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    when (currentScreen) {
                        ScreenState.DASHBOARD -> {
                            DashboardScreen(
                                repository = repository,
                                onNavigateToPlayground = { currentScreen = ScreenState.PLAYGROUND },
                                onNavigateToProfiles = { currentScreen = ScreenState.PROFILES },
                                onNavigateToGuide = { currentScreen = ScreenState.GUIDE },
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            )
                        }
                        ScreenState.PLAYGROUND -> {
                            PlaygroundScreen(
                                repository = repository,
                                onBack = { currentScreen = ScreenState.DASHBOARD },
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            )
                        }
                        ScreenState.PROFILES -> {
                            ProfilesScreen(
                                repository = repository,
                                onBack = { currentScreen = ScreenState.DASHBOARD },
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            )
                        }
                        ScreenState.GUIDE -> {
                            SetupGuideScreen(
                                onBack = { currentScreen = ScreenState.DASHBOARD },
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            )
                        }
                    }
                }
            }
        }
    }
}

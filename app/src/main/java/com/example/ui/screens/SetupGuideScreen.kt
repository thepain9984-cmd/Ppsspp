package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.MidnightBackground
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SetupGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("setup_guide_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "DOCUMENTATION",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Setup & Architecture Guide",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step 1: Android Studio Setup
        GuideStepCard(
            stepNumber = "1",
            title = "Android Studio Setup & Build",
            icon = Icons.Default.Build,
            description = "How to build and install Custom Controls onto your test device.",
            codeSnippet = "git clone <repo>\n./gradlew assembleDebug\n# Or Run 'app' directly in Android Studio"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BulletPoint("Minimum SDK: API 26 (Android 8.0) or API 28+ for continuous gesture strokes.")
                BulletPoint("Target SDK: Android 14+ (API 34/36) with foreground service specialUse configuration.")
                BulletPoint("No root or ADB wireless pairing required for daily usage!")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Permissions Configuration
        GuideStepCard(
            stepNumber = "2",
            title = "Permission Onboarding",
            icon = Icons.Default.Security,
            description = "Enabling the two required system permissions:",
            codeSnippet = "1. Settings > Apps > Special App Access > Display over other apps\n2. Settings > Accessibility > Custom Controls Touch Engine > ON"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BulletPoint("Draw over other apps: Allows WindowManager to render floating D-Pad & Action buttons.")
                BulletPoint("Accessibility Service: Allows dispatchGesture() to inject synthetic screen taps without requiring root privileges.")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Game Touch Mapping
        GuideStepCard(
            stepNumber = "3",
            title = "In-Game Mapping Workflow",
            icon = Icons.Default.Gamepad,
            description = "Calibrating the virtual controls for any game:",
            codeSnippet = null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BulletPoint("Launch Custom Controls and activate the overlay switch.")
                BulletPoint("Switch to your favorite mobile game (e.g. Genshin, CoD Mobile, PPSSPP).")
                BulletPoint("Tap the Edit icon on the top overlay bar to enter Edit Mode.")
                BulletPoint("Drag target dots directly on top of the in-game attack, jump, or joystick controls.")
                BulletPoint("Tap the Lock icon to save and start playing!")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 4: Touch Injection Architecture
        GuideStepCard(
            stepNumber = "4",
            title = "Under The Hood: dispatchGesture",
            icon = Icons.Default.Code,
            description = "How the touch engine simulates low-latency inputs:",
            codeSnippet = "val path = Path().apply { moveTo(targetX, targetY) }\nval stroke = StrokeDescription(path, 0L, 15L)\nservice.dispatchGesture(gesture, callback, null)"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BulletPoint("Instant Taps: Dispatches a 15ms stroke on the target coordinates.")
                BulletPoint("Analog Joystick: Dynamically calculates thumb angle and translates it into swipe trajectories.")
                BulletPoint("No lag queueing: Gesture queue throttles congested events to maintain real-time 60fps responsiveness.")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun GuideStepCard(
    stepNumber: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    codeSnippet: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(DeepSurface, CircleShape)
                            .border(1.dp, CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stepNumber, color = CyberCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Icon(imageVector = icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(description, color = TextSecondary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(10.dp))
            content()

            if (codeSnippet != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = DeepSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = codeSnippet,
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("• ", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(text, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

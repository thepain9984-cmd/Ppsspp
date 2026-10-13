package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GamepadRepository
import com.example.model.GamepadProfile
import com.example.service.GameTouchAccessibilityService
import com.example.service.OverlayService
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MidnightBackground
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    repository: GamepadRepository,
    onNavigateToPlayground: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProfile by repository.activeProfile.collectAsState()
    val isOverlayActive by repository.isOverlayActive.collectAsState()
    val isAccessibilityConnected by GameTouchAccessibilityService.isServiceConnected.collectAsState()
    val canDrawOverlay = Settings.canDrawOverlays(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Hero Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CUSTOM CONTROLS",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Gamepad Engine",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                color = SurfaceCard,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.size(44.dp).clickable { onNavigateToGuide() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = "Setup Guide",
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Master Toggle Card
        MasterToggleCard(
            isOverlayActive = isOverlayActive,
            canDrawOverlay = canDrawOverlay,
            isAccessibilityConnected = isAccessibilityConnected,
            onToggle = { enable ->
                if (enable) {
                    if (!canDrawOverlay) {
                        openOverlayPermissionSettings(context)
                    } else if (!isAccessibilityConnected) {
                        openAccessibilitySettings(context)
                    } else {
                        val intent = Intent(context, OverlayService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }
                } else {
                    val intent = Intent(context, OverlayService::class.java).apply {
                        action = OverlayService.ACTION_STOP_OVERLAY
                    }
                    context.startService(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Status & Permissions Grid
        Text(
            text = "SYSTEM PERMISSIONS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        PermissionItemCard(
            title = "Draw Over Other Apps",
            subtitle = "Required to float virtual gamepad over game screens",
            isGranted = canDrawOverlay,
            icon = Icons.Default.Layers,
            onClick = { openOverlayPermissionSettings(context) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionItemCard(
            title = "Touch Accessibility Engine",
            subtitle = "Required to synthesize game touches via dispatchGesture",
            isGranted = isAccessibilityConnected,
            icon = Icons.Default.TouchApp,
            onClick = { openAccessibilitySettings(context) }
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Active Profile Summary Card
        Text(
            text = "ACTIVE PRESET",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        ActiveProfileCard(
            profile = activeProfile,
            onManageProfiles = onNavigateToProfiles,
            onOpacityChanged = { newOpacity ->
                repository.updateProfile(activeProfile.copy(opacity = newOpacity))
            },
            onHapticToggled = { enabled ->
                repository.updateProfile(activeProfile.copy(hapticFeedback = enabled))
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Quick Navigation Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionTile(
                title = "Test Playground",
                subtitle = "Live touch feedback",
                icon = Icons.Default.Gamepad,
                accent = CyberCyan,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToPlayground
            )

            QuickActionTile(
                title = "Manage Presets",
                subtitle = "Switch game layouts",
                icon = Icons.Default.Tune,
                accent = Color(0xFF2979FF),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToProfiles
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun MasterToggleCard(
    isOverlayActive: Boolean,
    canDrawOverlay: Boolean,
    isAccessibilityConnected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val isReady = canDrawOverlay && isAccessibilityConnected

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOverlayActive) SurfaceCardElevated else SurfaceCard
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isOverlayActive) CyberCyan else BorderSubtle
        ),
        modifier = Modifier.fillMaxWidth().testTag("master_toggle_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isOverlayActive) SuccessGreen else Color(0xFF64748B),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isOverlayActive) "OVERLAY ACTIVE" else "OVERLAY STOPPED",
                        color = if (isOverlayActive) CyberCyan else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Switch(
                    checked = isOverlayActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DeepSurface
                    ),
                    modifier = Modifier.testTag("overlay_master_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isOverlayActive) {
                    "Virtual gamepad is floating on-screen. Switch to your game or use the floating bubble to adjust mappings."
                } else if (!isReady) {
                    "Setup required: Enable overlay & accessibility service below before activating floating gamepad."
                } else {
                    "Ready to launch! Turn on the switch to display the floating virtual gamepad over your games."
                },
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isGranted) Color(0xFF1E3A2F) else BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("permission_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(DeepSurface, CircleShape)
                        .border(1.dp, if (isGranted) SuccessGreen.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = if (isGranted) Color(0xFF064E3B) else Color(0xFF451A03),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isGranted) "ENABLED" else "GRANT",
                    color = if (isGranted) SuccessGreen else WarningAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun ActiveProfileCard(
    profile: GamepadProfile,
    onManageProfiles: () -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onHapticToggled: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth().testTag("active_profile_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = profile.name,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${profile.mappings.size} buttons configured",
                        color = CyberCyan,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onManageProfiles,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSurface),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Change", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Opacity slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Overlay Visibility", color = TextSecondary, fontSize = 12.sp)
                Text("${(profile.opacity * 100).roundToInt()}%", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = profile.opacity,
                onValueChange = onOpacityChanged,
                valueRange = 0.15f..0.95f,
                colors = SliderDefaults.colors(
                    thumbColor = CyberCyan,
                    activeTrackColor = CyberCyan,
                    inactiveTrackColor = DeepSurface
                )
            )

            // Haptic toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibration Feedback on Tap", color = TextSecondary, fontSize = 13.sp)
                Switch(
                    checked = profile.hapticFeedback,
                    onCheckedChange = onHapticToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DeepSurface
                    )
                )
            }
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DeepSurface, CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
    }
}

fun openOverlayPermissionSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

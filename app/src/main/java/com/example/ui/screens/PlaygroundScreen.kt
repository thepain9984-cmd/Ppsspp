package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GamepadRepository
import com.example.model.ButtonMapping
import com.example.model.ControlKey
import com.example.model.GamepadProfile
import com.example.service.GameTouchAccessibilityService
import com.example.ui.components.ActionButtonsCluster
import com.example.ui.components.AnalogJoystickView
import com.example.ui.components.DPadView
import com.example.ui.components.SystemPillButton
import com.example.ui.components.TriggerButtonView
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.MidnightBackground
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class VisualTouchRipple(
    val id: Long = System.currentTimeMillis() + (0..1000).random(),
    val xPx: Float,
    val yPx: Float,
    val color: Color = CyberCyan
)

@Composable
fun PlaygroundScreen(
    repository: GamepadRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProfile by repository.activeProfile.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    var selectedMappingForEdit by remember { mutableStateOf<ButtonMapping?>(null) }
    val ripples = remember { mutableStateListOf<VisualTouchRipple>() }
    val coroutineScope = rememberCoroutineScope()

    // Listen to touch events emitted by GameTouchAccessibilityService
    LaunchedEffect(Unit) {
        GameTouchAccessibilityService.simulatedTouchEvents.collectLatest { event ->
            ripples.add(VisualTouchRipple(xPx = event.x, yPx = event.y))
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .testTag("playground_screen")
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()

        // 1. Mock Game Arena Canvas Background (gives real context for mapping)
        MockGameArenaBackground(modifier = Modifier.fillMaxSize())

        // 2. Ripple Visualizer Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            ripples.forEach { ripple ->
                drawCircle(
                    color = ripple.color.copy(alpha = 0.85f),
                    radius = 28f,
                    center = Offset(ripple.xPx, ripple.yPx),
                    style = Stroke(width = 3.5f)
                )
                drawCircle(
                    color = ripple.color.copy(alpha = 0.35f),
                    radius = 16f,
                    center = Offset(ripple.xPx, ripple.yPx)
                )
            }

            // In Edit Mode, draw connection lines from virtual control anchors to target touch points
            if (isEditMode) {
                activeProfile.mappings.forEach { mapping ->
                    val controlX = mapping.overlayXPercent * totalWidth
                    val controlY = mapping.overlayYPercent * totalHeight
                    val targetX = mapping.targetXPercent * totalWidth
                    val targetY = mapping.targetYPercent * totalHeight

                    drawLine(
                        color = Color(0x7700E5FF),
                        start = Offset(controlX, controlY),
                        end = Offset(targetX, targetY),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                }
            }
        }

        // 3. Target Markers (Visible in Edit Mode and test mode)
        activeProfile.mappings.forEach { mapping ->
            val targetXPx = mapping.targetXPercent * totalWidth
            val targetYPx = mapping.targetYPercent * totalHeight

            DraggableTargetCrosshair(
                mapping = mapping,
                xPx = targetXPx,
                yPx = targetYPx,
                isDraggable = isEditMode,
                isSelected = selectedMappingForEdit?.key == mapping.key,
                onSelect = { selectedMappingForEdit = mapping },
                onMoved = { newNormX, newNormY ->
                    repository.updateMappingInActiveProfile(
                        mapping.copy(
                            targetXPercent = newNormX.coerceIn(0.04f, 0.96f),
                            targetYPercent = newNormY.coerceIn(0.04f, 0.96f)
                        )
                    )
                }
            )
        }

        // 4. Virtual Gamepad Overlay Controls (Rendered live in playground)
        // D-Pad & Joystick (Left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DPadView(
                sizeDp = 120.dp,
                alpha = activeProfile.opacity,
                onDirectionDown = { key ->
                    triggerControlAction(key, activeProfile, totalWidth, totalHeight, ripples)
                },
                onDirectionUp = {}
            )
            Spacer(modifier = Modifier.height(14.dp))
            AnalogJoystickView(
                sizeDp = 135.dp,
                alpha = activeProfile.opacity,
                onJoystickMove = { normX, normY ->
                    triggerJoystickAction(normX, normY, activeProfile, totalWidth, totalHeight, ripples)
                },
                onJoystickRelease = {}
            )
        }

        // System Buttons (Bottom center)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SystemPillButton(
                key = ControlKey.SELECT,
                alpha = activeProfile.opacity,
                onDown = { triggerControlAction(ControlKey.SELECT, activeProfile, totalWidth, totalHeight, ripples) },
                onUp = {}
            )
            SystemPillButton(
                key = ControlKey.START,
                alpha = activeProfile.opacity,
                onDown = { triggerControlAction(ControlKey.START, activeProfile, totalWidth, totalHeight, ripples) },
                onUp = {}
            )
        }

        // Action Quad (Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 44.dp)
        ) {
            ActionButtonsCluster(
                sizeDp = 160.dp,
                alpha = activeProfile.opacity,
                onButtonDown = { key ->
                    triggerControlAction(key, activeProfile, totalWidth, totalHeight, ripples)
                },
                onButtonUp = {}
            )
        }

        // Shoulder Bumpers (Top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 72.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TriggerButtonView(
                key = ControlKey.BUMPER_L1,
                alpha = activeProfile.opacity,
                onDown = { triggerControlAction(ControlKey.BUMPER_L1, activeProfile, totalWidth, totalHeight, ripples) },
                onUp = {}
            )
            TriggerButtonView(
                key = ControlKey.BUMPER_R1,
                alpha = activeProfile.opacity,
                onDown = { triggerControlAction(ControlKey.BUMPER_R1, activeProfile, totalWidth, totalHeight, ripples) },
                onUp = {}
            )
        }

        // 5. Top Playground Toolbar
        Surface(
            color = SurfaceCard.copy(alpha = 0.95f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3852)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Touch Playground", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (isEditMode) "Drag target dots to calibrate" else "Tap controls to test simulated touches",
                            color = if (isEditMode) Color(0xFFEF4444) else CyberCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { isEditMode = !isEditMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEditMode) Color(0xFFEF4444) else DeepSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Lock else Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isEditMode) Color.White else CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEditMode) "Lock Layout" else "Edit Targets",
                            color = if (isEditMode) Color.White else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mock mobile game arena rendered underneath the playground for realistic touch testing.
 */
@Composable
fun MockGameArenaBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Dark sci-fi game terrain grid
        drawRect(color = Color(0xFF0C101B))

        // Grid lines
        val step = 80f
        var x = 0f
        while (x < w) {
            drawLine(color = Color(0x1538BDF8), start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
            x += step
        }
        var y = 0f
        while (y < h) {
            drawLine(color = Color(0x1538BDF8), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
            y += step
        }

        // Mock game HUD: Health & Mana bars at top
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(40f, 150f),
            size = androidx.compose.ui.geometry.Size(220f, 14f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
        )
        drawRoundRect(
            color = Color(0xFF10B981),
            topLeft = Offset(40f, 150f),
            size = androidx.compose.ui.geometry.Size(180f, 14f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
        )

        // Mock game skill buttons at bottom-right
        drawCircle(
            color = Color(0x332563EB),
            radius = 48f,
            center = Offset(w * 0.84f, h * 0.75f)
        )
        drawCircle(
            color = Color(0x33DC2626),
            radius = 44f,
            center = Offset(w * 0.92f, h * 0.65f)
        )
        drawCircle(
            color = Color(0x33059669),
            radius = 40f,
            center = Offset(w * 0.84f, h * 0.55f)
        )
    }
}

/**
 * Target crosshair marker indicating where simulated touch will land on the game screen.
 */
@Composable
fun DraggableTargetCrosshair(
    mapping: ButtonMapping,
    xPx: Float,
    yPx: Float,
    isDraggable: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onMoved: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset((xPx - 22.dp.value * 2).roundToInt(), (yPx - 22.dp.value * 2).roundToInt())
            }
            .pointerInput(mapping.key, isDraggable) {
                if (isDraggable) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newNormX = (xPx + dragAmount.x) / 1080f // normalized
                        val newNormY = (yPx + dragAmount.y) / 2400f
                        onMoved(newNormX, newNormY)
                    }
                }
            }
            .size(44.dp)
            .background(
                if (isDraggable) Color(0xFFEF4444).copy(alpha = 0.45f) else Color(0xFF00E5FF).copy(alpha = 0.25f),
                CircleShape
            )
            .border(
                2.dp,
                if (isDraggable) Color(0xFFEF4444) else CyberCyan.copy(alpha = 0.8f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mapping.customLabel,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun triggerControlAction(
    key: ControlKey,
    profile: GamepadProfile,
    totalWidth: Float,
    totalHeight: Float,
    ripples: MutableList<VisualTouchRipple>
) {
    val mapping = profile.mappings.find { it.key == key && it.isEnabled } ?: return
    val targetX = mapping.targetXPercent * totalWidth
    val targetY = mapping.targetYPercent * totalHeight

    ripples.add(VisualTouchRipple(xPx = targetX, yPx = targetY))
    GameTouchAccessibilityService.performTap(targetX, targetY, durationMs = 15L)
}

private fun triggerJoystickAction(
    normX: Float,
    normY: Float,
    profile: GamepadProfile,
    totalWidth: Float,
    totalHeight: Float,
    ripples: MutableList<VisualTouchRipple>
) {
    val mapping = profile.mappings.find { it.key == ControlKey.JOYSTICK && it.isEnabled } ?: return
    val centerX = mapping.targetXPercent * totalWidth
    val centerY = mapping.targetYPercent * totalHeight
    val maxRadius = mapping.targetRadiusPercent * totalWidth

    val targetX = centerX + (normX * maxRadius)
    val targetY = centerY + (normY * maxRadius)

    ripples.add(VisualTouchRipple(xPx = targetX, yPx = targetY, color = Color(0xFF38BDF8)))
    GameTouchAccessibilityService.performDrag(centerX, centerY, targetX, targetY, durationMs = 25L)
}

package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AnalogJoystickView(
    sizeDp: Dp = 150.dp,
    alpha: Float = 0.70f,
    onJoystickMove: (normX: Float, normY: Float) -> Unit,
    onJoystickRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val thumbOffsetX = remember { Animatable(0f) }
    val thumbOffsetY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val baseRadiusPx = (sizeDp.value * 1.5f) // Approximate conversion factor
    val maxRadiusPx = (sizeDp.value * 0.95f)

    Box(
        modifier = modifier
            .size(sizeDp)
            .testTag("analog_joystick")
            .pointerInteropFilter { event ->
                val centerX = event.x - (sizeDp.value / 2f) // rough local mapping
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = true
                        coroutineScope.launch {
                            thumbOffsetX.snapTo(0f)
                            thumbOffsetY.snapTo(0f)
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // Using raw touch delta relative to box center
                        val localX = event.x - (sizeDp.value)
                        val localY = event.y - (sizeDp.value)
                        val distance = hypot(localX, localY)
                        val maxDistance = sizeDp.value * 0.75f

                        val clampedX: Float
                        val clampedY: Float
                        if (distance > maxDistance && distance > 0f) {
                            clampedX = (localX / distance) * maxDistance
                            clampedY = (localY / distance) * maxDistance
                        } else {
                            clampedX = localX
                            clampedY = localY
                        }

                        coroutineScope.launch {
                            thumbOffsetX.snapTo(clampedX)
                            thumbOffsetY.snapTo(clampedY)
                        }

                        val normX = (clampedX / maxDistance).coerceIn(-1f, 1f)
                        val normY = (clampedY / maxDistance).coerceIn(-1f, 1f)
                        onJoystickMove(normX, normY)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                        onJoystickRelease()
                        coroutineScope.launch {
                            thumbOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        coroutineScope.launch {
                            thumbOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    }
                }
                true
            },
        contentAlignment = Alignment.Center
    ) {
        // Base ring canvas
        Canvas(modifier = Modifier.size(sizeDp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - 4f

            // Outer boundary ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E293B).copy(alpha = alpha * 0.85f),
                        Color(0xFF0F172A).copy(alpha = alpha)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Cyber neon accent border
            drawCircle(
                color = if (isDragging) CyberCyan.copy(alpha = 0.9f) else Color(0xFF334155).copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = if (isDragging) 2.5f else 1.5f)
            )

            // Inner crosshair ticks
            val tickColor = Color(0xFF475569).copy(alpha = alpha)
            drawLine(
                color = tickColor,
                start = Offset(center.x, center.y - radius * 0.85f),
                end = Offset(center.x, center.y - radius * 0.55f),
                strokeWidth = 2f
            )
            drawLine(
                color = tickColor,
                start = Offset(center.x, center.y + radius * 0.55f),
                end = Offset(center.x, center.y + radius * 0.85f),
                strokeWidth = 2f
            )
            drawLine(
                color = tickColor,
                start = Offset(center.x - radius * 0.85f, center.y),
                end = Offset(center.x - radius * 0.55f, center.y),
                strokeWidth = 2f
            )
            drawLine(
                color = tickColor,
                start = Offset(center.x + radius * 0.55f, center.y),
                end = Offset(center.x + radius * 0.85f, center.y),
                strokeWidth = 2f
            )
        }

        // Draggable Thumb nub
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        thumbOffsetX.value.roundToInt(),
                        thumbOffsetY.value.roundToInt()
                    )
                }
                .size(sizeDp * 0.44f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(sizeDp * 0.44f)) {
                val nubCenter = Offset(size.width / 2f, size.height / 2f)
                val nubRadius = size.width / 2f - 2f

                // Thumbstick cap surface
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B4861).copy(alpha = alpha + 0.15f),
                            Color(0xFF1E2638).copy(alpha = alpha + 0.2f)
                        ),
                        center = nubCenter,
                        radius = nubRadius
                    ),
                    radius = nubRadius,
                    center = nubCenter
                )

                // Concave center depression
                drawCircle(
                    color = Color(0xFF131A29).copy(alpha = alpha + 0.2f),
                    radius = nubRadius * 0.65f,
                    center = nubCenter
                )

                // Neon active dot
                drawCircle(
                    color = if (isDragging) CyberCyan else Color(0xFF64748B),
                    radius = nubRadius * 0.22f,
                    center = nubCenter
                )

                drawCircle(
                    color = if (isDragging) CyberCyan.copy(alpha = 0.8f) else Color(0xFF475569),
                    radius = nubRadius,
                    center = nubCenter,
                    style = Stroke(width = 1.8f)
                )
            }
        }
    }
}

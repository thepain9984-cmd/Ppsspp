package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControlKey
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DPadActive
import com.example.ui.theme.DPadNeutral
import kotlin.math.atan2
import kotlin.math.hypot

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DPadView(
    sizeDp: Dp = 140.dp,
    alpha: Float = 0.75f,
    onDirectionDown: (ControlKey) -> Unit,
    onDirectionUp: (ControlKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeDirection by remember { mutableStateOf<ControlKey?>(null) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .testTag("dpad_controller")
            .pointerInteropFilter { event ->
                val centerX = sizeDp.value / 2f
                val centerY = sizeDp.value / 2f
                val dx = event.x - (event.x.coerceIn(0f, sizeDp.value * 2) / 2f) // fallback or raw event
                val touchX = event.x
                val touchY = event.y
                val dist = hypot(touchX - centerX, touchY - centerY)

                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        val angle = Math.toDegrees(atan2((touchY - centerY).toDouble(), (touchX - centerX).toDouble()))
                        val dir = when {
                            angle in -135.0..-45.0 -> ControlKey.DPAD_UP
                            angle in -45.0..45.0 -> ControlKey.DPAD_RIGHT
                            angle in 45.0..135.0 -> ControlKey.DPAD_DOWN
                            else -> ControlKey.DPAD_LEFT
                        }

                        if (dir != activeDirection) {
                            activeDirection?.let { onDirectionUp(it) }
                            activeDirection = dir
                            onDirectionDown(dir)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        activeDirection?.let { onDirectionUp(it) }
                        activeDirection = null
                    }
                }
                true
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val w = size.width
            val h = size.height
            val armWidth = w * 0.34f
            val armLength = w * 0.48f

            // Cross base shadow & body
            val basePath = Path().apply {
                // Horizontal bar
                addRect(androidx.compose.ui.geometry.Rect(
                    Offset((w - armLength) / 2f, (h - armWidth) / 2f),
                    Size(armLength, armWidth)
                ))
                // Vertical bar
                addRect(androidx.compose.ui.geometry.Rect(
                    Offset((w - armWidth) / 2f, (h - armLength) / 2f),
                    Size(armWidth, armLength)
                ))
            }

            drawPath(
                path = basePath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF26334A).copy(alpha = alpha),
                        Color(0xFF141C2B).copy(alpha = alpha)
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = w / 2f
                )
            )

            // Draw directional arrows & active highlights
            val isUp = activeDirection == ControlKey.DPAD_UP
            val isDown = activeDirection == ControlKey.DPAD_DOWN
            val isLeft = activeDirection == ControlKey.DPAD_LEFT
            val isRight = activeDirection == ControlKey.DPAD_RIGHT

            // Arrow Up
            drawPath(
                path = Path().apply {
                    moveTo(w / 2f, h * 0.12f)
                    lineTo(w / 2f - 14f, h * 0.22f)
                    lineTo(w / 2f + 14f, h * 0.22f)
                    close()
                },
                color = if (isUp) CyberCyan else DPadNeutral.copy(alpha = 0.85f)
            )

            // Arrow Down
            drawPath(
                path = Path().apply {
                    moveTo(w / 2f, h * 0.88f)
                    lineTo(w / 2f - 14f, h * 0.78f)
                    lineTo(w / 2f + 14f, h * 0.78f)
                    close()
                },
                color = if (isDown) CyberCyan else DPadNeutral.copy(alpha = 0.85f)
            )

            // Arrow Left
            drawPath(
                path = Path().apply {
                    moveTo(w * 0.12f, h / 2f)
                    lineTo(w * 0.22f, h / 2f - 14f)
                    lineTo(w * 0.22f, h / 2f + 14f)
                    close()
                },
                color = if (isLeft) CyberCyan else DPadNeutral.copy(alpha = 0.85f)
            )

            // Arrow Right
            drawPath(
                path = Path().apply {
                    moveTo(w * 0.88f, h / 2f)
                    lineTo(w * 0.78f, h / 2f - 14f)
                    lineTo(w * 0.78f, h / 2f + 14f)
                    close()
                },
                color = if (isRight) CyberCyan else DPadNeutral.copy(alpha = 0.85f)
            )

            // Center pivot circular depression
            drawCircle(
                color = Color(0xFF101726).copy(alpha = alpha),
                radius = armWidth * 0.38f,
                center = Offset(w / 2f, h / 2f)
            )
        }
    }
}

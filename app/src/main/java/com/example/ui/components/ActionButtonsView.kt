package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ControlKey
import com.example.ui.theme.GlyphCircleCoral
import com.example.ui.theme.GlyphCrossBlue
import com.example.ui.theme.GlyphSquareMagenta
import com.example.ui.theme.GlyphTriangleGreen

@Composable
fun ActionButtonsCluster(
    sizeDp: Dp = 150.dp,
    alpha: Float = 0.75f,
    onButtonDown: (ControlKey) -> Unit,
    onButtonUp: (ControlKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = sizeDp * 0.38f
    val spacing = sizeDp * 0.31f

    Box(
        modifier = modifier
            .size(sizeDp)
            .testTag("action_buttons_cluster"),
        contentAlignment = Alignment.Center
    ) {
        // Triangle (Top)
        ActionButtonItem(
            key = ControlKey.ACTION_TRIANGLE,
            glyphColor = GlyphTriangleGreen,
            sizeDp = buttonSize,
            alpha = alpha,
            onDown = { onButtonDown(ControlKey.ACTION_TRIANGLE) },
            onUp = { onButtonUp(ControlKey.ACTION_TRIANGLE) },
            modifier = Modifier.offset(y = -spacing)
        )

        // Circle (Right)
        ActionButtonItem(
            key = ControlKey.ACTION_CIRCLE,
            glyphColor = GlyphCircleCoral,
            sizeDp = buttonSize,
            alpha = alpha,
            onDown = { onButtonDown(ControlKey.ACTION_CIRCLE) },
            onUp = { onButtonUp(ControlKey.ACTION_CIRCLE) },
            modifier = Modifier.offset(x = spacing)
        )

        // Cross (Bottom)
        ActionButtonItem(
            key = ControlKey.ACTION_CROSS,
            glyphColor = GlyphCrossBlue,
            sizeDp = buttonSize,
            alpha = alpha,
            onDown = { onButtonDown(ControlKey.ACTION_CROSS) },
            onUp = { onButtonUp(ControlKey.ACTION_CROSS) },
            modifier = Modifier.offset(y = spacing)
        )

        // Square (Left)
        ActionButtonItem(
            key = ControlKey.ACTION_SQUARE,
            glyphColor = GlyphSquareMagenta,
            sizeDp = buttonSize,
            alpha = alpha,
            onDown = { onButtonDown(ControlKey.ACTION_SQUARE) },
            onUp = { onButtonUp(ControlKey.ACTION_SQUARE) },
            modifier = Modifier.offset(x = -spacing)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ActionButtonItem(
    key: ControlKey,
    glyphColor: Color,
    sizeDp: Dp,
    alpha: Float,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .testTag("btn_${key.name.lowercase()}")
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onDown()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        onUp()
                    }
                }
                true
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - 2f

            // Button body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isPressed) {
                        listOf(glyphColor.copy(alpha = 0.5f), Color(0xFF1E293B).copy(alpha = alpha + 0.2f))
                    } else {
                        listOf(Color(0xFF26334A).copy(alpha = alpha), Color(0xFF131A29).copy(alpha = alpha))
                    },
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Outer border
            drawCircle(
                color = if (isPressed) glyphColor else Color(0xFF475569).copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = if (isPressed) 2.5f else 1.5f)
            )

            // Draw Glyph symbol
            val glyphSize = radius * 0.55f
            when (key) {
                ControlKey.ACTION_TRIANGLE -> {
                    val path = Path().apply {
                        moveTo(center.x, center.y - glyphSize)
                        lineTo(center.x + glyphSize * 0.866f, center.y + glyphSize * 0.5f)
                        lineTo(center.x - glyphSize * 0.866f, center.y + glyphSize * 0.5f)
                        close()
                    }
                    drawPath(path, color = glyphColor, style = Stroke(width = 3.5f))
                }
                ControlKey.ACTION_CIRCLE -> {
                    drawCircle(
                        color = glyphColor,
                        radius = glyphSize * 0.75f,
                        center = center,
                        style = Stroke(width = 3.5f)
                    )
                }
                ControlKey.ACTION_CROSS -> {
                    val s = glyphSize * 0.65f
                    drawLine(
                        color = glyphColor,
                        start = Offset(center.x - s, center.y - s),
                        end = Offset(center.x + s, center.y + s),
                        strokeWidth = 3.8f
                    )
                    drawLine(
                        color = glyphColor,
                        start = Offset(center.x + s, center.y - s),
                        end = Offset(center.x - s, center.y + s),
                        strokeWidth = 3.8f
                    )
                }
                ControlKey.ACTION_SQUARE -> {
                    val s = glyphSize * 0.62f
                    drawRect(
                        color = glyphColor,
                        topLeft = Offset(center.x - s, center.y - s),
                        size = androidx.compose.ui.geometry.Size(s * 2f, s * 2f),
                        style = Stroke(width = 3.5f)
                    )
                }
                else -> Unit
            }
        }
    }
}

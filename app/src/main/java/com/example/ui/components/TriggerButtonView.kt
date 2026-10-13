package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControlKey
import com.example.ui.theme.CyberCyan

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TriggerButtonView(
    key: ControlKey,
    label: String = key.defaultLabel,
    widthDp: Dp = 76.dp,
    heightDp: Dp = 40.dp,
    alpha: Float = 0.75f,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(widthDp)
            .height(heightDp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("trigger_${key.name.lowercase()}")
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
        Canvas(modifier = Modifier.size(widthDp, heightDp)) {
            val w = size.width
            val h = size.height

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(CyberCyan.copy(alpha = 0.45f), Color(0xFF1B2433).copy(alpha = alpha + 0.2f))
                    } else {
                        listOf(Color(0xFF28344A).copy(alpha = alpha), Color(0xFF131A26).copy(alpha = alpha))
                    }
                ),
                cornerRadius = CornerRadius(24f, 24f)
            )

            drawRoundRect(
                color = if (isPressed) CyberCyan else Color(0xFF475569).copy(alpha = alpha),
                style = Stroke(width = if (isPressed) 2.5f else 1.5f),
                cornerRadius = CornerRadius(24f, 24f)
            )
        }

        Text(
            text = label,
            color = if (isPressed) CyberCyan else Color(0xFFE2E8F0),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

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
fun SystemPillButton(
    key: ControlKey,
    label: String = key.defaultLabel,
    widthDp: Dp = 60.dp,
    heightDp: Dp = 26.dp,
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
            .clip(RoundedCornerShape(13.dp))
            .testTag("sys_${key.name.lowercase()}")
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
            drawRoundRect(
                color = if (isPressed) Color(0xFF2E3D52) else Color(0xFF192233).copy(alpha = alpha),
                cornerRadius = CornerRadius(20f, 20f)
            )
            drawRoundRect(
                color = if (isPressed) CyberCyan else Color(0xFF475569).copy(alpha = alpha),
                style = Stroke(width = if (isPressed) 2f else 1.2f),
                cornerRadius = CornerRadius(20f, 20f)
            )
        }

        Text(
            text = label,
            color = if (isPressed) CyberCyan else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

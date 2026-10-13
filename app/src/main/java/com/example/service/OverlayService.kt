package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.GamepadRepository
import com.example.model.ButtonMapping
import com.example.model.ControlKey
import com.example.model.GamepadProfile
import com.example.ui.components.ActionButtonItem
import com.example.ui.components.ActionButtonsCluster
import com.example.ui.components.AnalogJoystickView
import com.example.ui.components.DPadView
import com.example.ui.components.SystemPillButton
import com.example.ui.components.TriggerButtonView
import com.example.ui.theme.CustomControlsTheme
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GlyphCircleCoral
import com.example.ui.theme.GlyphCrossBlue
import com.example.ui.theme.GlyphSquareMagenta
import com.example.ui.theme.GlyphTriangleGreen
import kotlin.math.roundToInt

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: GamepadRepository
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private var overlayView: ComposeView? = null
    private var bubbleView: ComposeView? = null

    private var screenWidthPx = 1080
    private var screenHeightPx = 2400

    private val isMinimized = mutableStateOf(false)
    private val isEditMode = mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repository = GamepadRepository.getInstance(applicationContext)

        lifecycleOwner = OverlayLifecycleOwner().apply { onCreate() }

        updateScreenDimensions()
        startAsForegroundService()
        initOverlayViews()

        repository.setOverlayActive(true)
        Log.d(TAG, "OverlayService started successfully")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_EDIT -> {
                isEditMode.value = !isEditMode.value
                repository.setEditModeActive(isEditMode.value)
            }
        }
        return START_STICKY
    }

    private fun updateScreenDimensions() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidthPx = metrics.widthPixels
        screenHeightPx = metrics.heightPixels
    }

    private fun startAsForegroundService() {
        val channelId = "custom_controls_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gamepad Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the virtual gamepad overlay active over games"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Custom Controls Gamepad Active")
            .setContentText("Virtual overlay running. Tap to open profile settings.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initOverlayViews() {
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted!")
            stopSelf()
            return
        }

        // Create Full HUD Overlay
        overlayView = ComposeView(this).apply {
            lifecycleOwner?.attachToView(this)
            setContent {
                CustomControlsTheme {
                    val activeProfile by repository.activeProfile.collectAsState()
                    val editMode by isEditMode
                    val minimized by isMinimized

                    if (!minimized) {
                        GamepadOverlayContent(
                            profile = activeProfile,
                            isEditMode = editMode,
                            onMinimize = {
                                isMinimized.value = true
                                updateViewsVisibility()
                            },
                            onToggleEditMode = {
                                isEditMode.value = !isEditMode.value
                                repository.setEditModeActive(isEditMode.value)
                            },
                            onClose = { stopSelf() },
                            onButtonDown = { key -> handleButtonDown(key, activeProfile) },
                            onButtonUp = { key -> handleButtonUp(key, activeProfile) },
                            onJoystickDrag = { normX, normY -> handleJoystickDrag(normX, normY, activeProfile) },
                            onJoystickRelease = { handleJoystickRelease(activeProfile) },
                            onUpdateTargetPosition = { key, newNormX, newNormY ->
                                val mapping = activeProfile.mappings.find { it.key == key }
                                if (mapping != null) {
                                    repository.updateMappingInActiveProfile(
                                        mapping.copy(
                                            targetXPercent = newNormX.coerceIn(0.02f, 0.98f),
                                            targetYPercent = newNormY.coerceIn(0.02f, 0.98f)
                                        )
                                    )
                                }
                            },
                            onUpdateOpacity = { newOpacity ->
                                repository.updateProfile(activeProfile.copy(opacity = newOpacity))
                            }
                        )
                    }
                }
            }
        }

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(overlayView, overlayParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlayView to WindowManager", e)
        }

        // Create Floating Bubble View (when minimized)
        bubbleView = ComposeView(this).apply {
            lifecycleOwner?.attachToView(this)
            setContent {
                CustomControlsTheme {
                    val minimized by isMinimized
                    if (minimized) {
                        FloatingBubble(
                            onClick = {
                                isMinimized.value = false
                                updateViewsVisibility()
                            }
                        )
                    }
                }
            }
        }

        val bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 300
        }

        try {
            windowManager.addView(bubbleView, bubbleParams)
            bubbleView?.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bubbleView to WindowManager", e)
        }
    }

    private fun updateViewsVisibility() {
        if (isMinimized.value) {
            overlayView?.visibility = View.GONE
            bubbleView?.visibility = View.VISIBLE
        } else {
            overlayView?.visibility = View.VISIBLE
            bubbleView?.visibility = View.GONE
        }
    }

    private fun handleButtonDown(key: ControlKey, profile: GamepadProfile) {
        triggerHaptic(profile)
        val mapping = profile.mappings.find { it.key == key && it.isEnabled } ?: return
        val targetX = mapping.targetXPercent * screenWidthPx
        val targetY = mapping.targetYPercent * screenHeightPx

        if (mapping.isHoldSupported) {
            GameTouchAccessibilityService.performHold(targetX, targetY, durationMs = 150L)
        } else {
            GameTouchAccessibilityService.performTap(targetX, targetY, durationMs = 15L)
        }
    }

    private fun handleButtonUp(key: ControlKey, profile: GamepadProfile) {
        // Accessibility gesture handles completion; nothing needed on up for instant taps
    }

    private fun handleJoystickDrag(normX: Float, normY: Float, profile: GamepadProfile) {
        val mapping = profile.mappings.find { it.key == ControlKey.JOYSTICK && it.isEnabled } ?: return
        val centerX = mapping.targetXPercent * screenWidthPx
        val centerY = mapping.targetYPercent * screenHeightPx
        val maxRadius = mapping.targetRadiusPercent * screenWidthPx

        val targetX = centerX + (normX * maxRadius)
        val targetY = centerY + (normY * maxRadius)

        GameTouchAccessibilityService.performDrag(
            startX = centerX,
            startY = centerY,
            endX = targetX,
            endY = targetY,
            durationMs = 25L
        )
    }

    private fun handleJoystickRelease(profile: GamepadProfile) {
        // Released back to center
    }

    private fun triggerHaptic(profile: GamepadProfile) {
        if (!profile.hapticFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(15L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(15L)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration error
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.setOverlayActive(false)
        repository.setEditModeActive(false)
        try {
            overlayView?.let { windowManager.removeView(it) }
            bubbleView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay views", e)
        }
        lifecycleOwner?.onDestroy()
        Log.d(TAG, "OverlayService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"
        const val ACTION_TOGGLE_EDIT = "com.example.action.TOGGLE_EDIT"
    }
}

/**
 * Top-level Gamepad Overlay HUD layout.
 */
@Composable
fun GamepadOverlayContent(
    profile: GamepadProfile,
    isEditMode: Boolean,
    onMinimize: () -> Unit,
    onToggleEditMode: () -> Unit,
    onClose: () -> Unit,
    onButtonDown: (ControlKey) -> Unit,
    onButtonUp: (ControlKey) -> Unit,
    onJoystickDrag: (Float, Float) -> Unit,
    onJoystickRelease: () -> Unit,
    onUpdateTargetPosition: (ControlKey, Float, Float) -> Unit,
    onUpdateOpacity: (Float) -> Unit
) {
    var showQuickSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // 1. Top HUD Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Bumper & Mode Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                TriggerButtonView(
                    key = ControlKey.BUMPER_L1,
                    alpha = profile.opacity,
                    onDown = { onButtonDown(ControlKey.BUMPER_L1) },
                    onUp = { onButtonUp(ControlKey.BUMPER_L1) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                if (isEditMode) {
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "EDIT MODE: DRAG TARGETS",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Center Quick Toolbar
            Surface(
                color = Color(0xFF131926).copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3852))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleEditMode,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Lock else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Lock Mapping" else "Edit Mapping",
                            tint = if (isEditMode) CyberCyan else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showQuickSettings = !showQuickSettings },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (showQuickSettings) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Quick Opacity",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Minimize Gamepad",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Gamepad",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Right Bumper
            TriggerButtonView(
                key = ControlKey.BUMPER_R1,
                alpha = profile.opacity,
                onDown = { onButtonDown(ControlKey.BUMPER_R1) },
                onUp = { onButtonUp(ControlKey.BUMPER_R1) }
            )
        }

        // Quick Opacity Slider Popup
        AnimatedVisibility(
            visible = showQuickSettings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        ) {
            Surface(
                color = Color(0xFF131926).copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Opacity", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${(profile.opacity * 100).roundToInt()}%", color = CyberCyan, fontSize = 12.sp)
                    }
                    Slider(
                        value = profile.opacity,
                        onValueChange = onUpdateOpacity,
                        valueRange = 0.15f..0.95f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }
            }
        }

        // 2. Left Controls: D-Pad & Analog Joystick
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DPadView(
                sizeDp = 130.dp,
                alpha = profile.opacity,
                onDirectionDown = onButtonDown,
                onDirectionUp = onButtonUp
            )
            Spacer(modifier = Modifier.height(16.dp))
            AnalogJoystickView(
                sizeDp = 140.dp,
                alpha = profile.opacity,
                onJoystickMove = onJoystickDrag,
                onJoystickRelease = onJoystickRelease
            )
        }

        // 3. Center Bottom Controls: SELECT & START
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SystemPillButton(
                key = ControlKey.SELECT,
                alpha = profile.opacity,
                onDown = { onButtonDown(ControlKey.SELECT) },
                onUp = { onButtonUp(ControlKey.SELECT) }
            )
            SystemPillButton(
                key = ControlKey.START,
                alpha = profile.opacity,
                onDown = { onButtonDown(ControlKey.START) },
                onUp = { onButtonUp(ControlKey.START) }
            )
        }

        // 4. Right Controls: Action Quad (Triangle, Circle, Cross, Square)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 48.dp)
        ) {
            ActionButtonsCluster(
                sizeDp = 170.dp,
                alpha = profile.opacity,
                onButtonDown = onButtonDown,
                onButtonUp = onButtonUp
            )
        }

        // 5. Edit Mode Target Overlays (shows draggable target anchors on the screen)
        if (isEditMode) {
            profile.mappings.forEach { mapping ->
                DraggableTargetAnchor(
                    mapping = mapping,
                    onPositionChanged = { normX, normY ->
                        onUpdateTargetPosition(mapping.key, normX, normY)
                    }
                )
            }
        }
    }
}

/**
 * Draggable target touch point crosshair shown during Edit Mode.
 */
@Composable
fun DraggableTargetAnchor(
    mapping: ButtonMapping,
    onPositionChanged: (Float, Float) -> Unit
) {
    var currentNormX by remember(mapping.targetXPercent) { mutableFloatStateOf(mapping.targetXPercent) }
    var currentNormY by remember(mapping.targetYPercent) { mutableFloatStateOf(mapping.targetYPercent) }

    Box(
        modifier = Modifier
            .offset {
                // Approximate screen bounds
                IntOffset(
                    (currentNormX * 1000f).roundToInt(),
                    (currentNormY * 2000f).roundToInt()
                )
            }
            .pointerInput(mapping.key) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentNormX += dragAmount.x / 1000f
                    currentNormY += dragAmount.y / 2000f
                    onPositionChanged(currentNormX, currentNormY)
                }
            }
            .size(44.dp)
            .background(Color(0xFFEF4444).copy(alpha = 0.4f), CircleShape)
            .border(2.dp, Color(0xFFEF4444), CircleShape),
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

/**
 * Minimized floating bubble docked on screen edge.
 */
@Composable
fun FloatingBubble(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(2.dp, CyberCyan),
        shadowElevation = 8.dp,
        modifier = Modifier.size(54.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Gamepad,
                contentDescription = "Expand Gamepad",
                tint = CyberCyan,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

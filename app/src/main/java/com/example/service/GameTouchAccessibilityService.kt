package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class GameTouchAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        Log.d(TAG, "GameTouchAccessibilityService connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used; we solely simulate gestures
    }

    override fun onInterrupt() {
        Log.w(TAG, "GameTouchAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        _isServiceConnected.value = false
        Log.d(TAG, "GameTouchAccessibilityService destroyed")
    }

    companion object {
        private const val TAG = "GameTouchAccessibility"

        @Volatile
        private var instance: GameTouchAccessibilityService? = null

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        // Emits simulated touch events so the in-app playground can render live ripples
        private val _simulatedTouchEvents = MutableSharedFlow<SimulatedTouchInfo>(extraBufferCapacity = 64)
        val simulatedTouchEvents: SharedFlow<SimulatedTouchInfo> = _simulatedTouchEvents.asSharedFlow()

        private val isDispatching = AtomicBoolean(false)

        data class SimulatedTouchInfo(
            val x: Float,
            val y: Float,
            val actionType: String, // "TAP", "HOLD", "DRAG", "RELEASE"
            val timestamp: Long = System.currentTimeMillis()
        )

        fun isConnected(): Boolean = instance != null

        /**
         * Simulates a single instant tap at the specified screen coordinates.
         */
        fun performTap(x: Float, y: Float, durationMs: Long = 15L, onResult: ((Boolean) -> Unit)? = null) {
            val service = instance
            // Notify visualizer regardless
            _simulatedTouchEvents.tryEmit(SimulatedTouchInfo(x, y, "TAP"))

            if (service == null) {
                Log.w(TAG, "Cannot dispatch tap: AccessibilityService is not running or enabled")
                onResult?.invoke(false)
                return
            }

            try {
                val path = Path().apply {
                    moveTo(x, y)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(1L, 100L))
                val gesture = GestureDescription.Builder()
                    .addStroke(stroke)
                    .build()

                service.dispatchGesture(
                    gesture,
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            onResult?.invoke(true)
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            Log.w(TAG, "Tap gesture cancelled at ($x, $y)")
                            onResult?.invoke(false)
                        }
                    },
                    null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching tap at ($x, $y)", e)
                onResult?.invoke(false)
            }
        }

        /**
         * Simulates a continuous swipe or drag between start and end coordinates (e.g. for Analog Joystick).
         */
        fun performDrag(
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            durationMs: Long = 25L,
            onResult: ((Boolean) -> Unit)? = null
        ) {
            val service = instance
            _simulatedTouchEvents.tryEmit(SimulatedTouchInfo(endX, endY, "DRAG"))

            if (service == null) {
                onResult?.invoke(false)
                return
            }

            if (isDispatching.getAndSet(true)) {
                // Skip if congested to prevent gesture backpressure
                return
            }

            try {
                val path = Path().apply {
                    moveTo(startX, startY)
                    lineTo(endX, endY)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(10L, 80L))
                val gesture = GestureDescription.Builder()
                    .addStroke(stroke)
                    .build()

                service.dispatchGesture(
                    gesture,
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            isDispatching.set(false)
                            onResult?.invoke(true)
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            isDispatching.set(false)
                            onResult?.invoke(false)
                        }
                    },
                    null
                )
            } catch (e: Exception) {
                isDispatching.set(false)
                Log.e(TAG, "Error dispatching drag gesture", e)
                onResult?.invoke(false)
            }
        }

        /**
         * Simulates a sustained press/hold at the specified coordinates.
         */
        fun performHold(x: Float, y: Float, durationMs: Long = 100L) {
            val service = instance
            _simulatedTouchEvents.tryEmit(SimulatedTouchInfo(x, y, "HOLD"))

            if (service == null) return

            try {
                val path = Path().apply {
                    moveTo(x, y)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(20L, 250L))
                val gesture = GestureDescription.Builder()
                    .addStroke(stroke)
                    .build()

                service.dispatchGesture(gesture, null, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching hold gesture at ($x, $y)", e)
            }
        }
    }
}

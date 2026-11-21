package com.example.wristgestures.service

import com.example.wristgestures.gesture.Gesture
import kotlinx.coroutines.flow.MutableStateFlow

data class DetectedGesture(val gesture: Gesture, val atMillis: Long)

/** Shared state between the background service and the watch screen. */
object GestureEvents {
    val running = MutableStateFlow(false)
    val lastGesture = MutableStateFlow<DetectedGesture?>(null)

    /** True while the app's main screen is showing. */
    @Volatile
    var uiVisible = false
}

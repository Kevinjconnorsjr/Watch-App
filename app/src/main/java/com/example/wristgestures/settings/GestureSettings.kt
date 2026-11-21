package com.example.wristgestures.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.wristgestures.action.WatchAction
import com.example.wristgestures.gesture.Gesture

enum class Sensitivity(val label: String, val factor: Float) {
    LOW("Low", 0.75f),
    MEDIUM("Medium", 1f),
    HIGH("High", 1.3f);

    fun next(): Sensitivity = entries[(ordinal + 1) % entries.size]
}

/** Remembers which action each gesture performs, plus the sensitivity. */
class GestureSettings(context: Context) {
    val prefs: SharedPreferences =
        context.getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)

    fun actionFor(gesture: Gesture): WatchAction =
        prefs.getString(key(gesture), null)
            ?.let { saved -> WatchAction.entries.firstOrNull { it.name == saved } }
            ?: defaultAction(gesture)

    fun setAction(gesture: Gesture, action: WatchAction) =
        prefs.edit { putString(key(gesture), action.name) }

    var sensitivity: Sensitivity
        get() = prefs.getString(KEY_SENSITIVITY, null)
            ?.let { saved -> Sensitivity.entries.firstOrNull { it.name == saved } }
            ?: Sensitivity.MEDIUM
        set(value) = prefs.edit { putString(KEY_SENSITIVITY, value.name) }

    private fun key(gesture: Gesture) = "action_${gesture.name}"

    companion object {
        const val KEY_SENSITIVITY = "sensitivity"

        fun defaultAction(gesture: Gesture): WatchAction = when (gesture) {
            Gesture.FLICK -> WatchAction.BEEP
            Gesture.SHAKE -> WatchAction.FLASH
            Gesture.TWIST -> WatchAction.VIBRATE
        }
    }
}

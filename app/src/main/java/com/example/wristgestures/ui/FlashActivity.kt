package com.example.wristgestures.ui

import android.app.Activity
import android.app.NotificationManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import com.example.wristgestures.action.ActionRunner

/**
 * Flashes the whole screen white for about 1.5 seconds, then closes.
 * Flashes are kept to 2 per second, below the 3-per-second rate that can trigger
 * photosensitive seizures.
 */
class FlashActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var view: View
    private var frame = 0

    private val step = object : Runnable {
        override fun run() {
            if (frame >= FRAMES) {
                finish()
                return
            }
            view.setBackgroundColor(if (frame % 2 == 0) Color.WHITE else Color.BLACK)
            frame++
            handler.postDelayed(this, FRAME_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 1f }

        view = View(this)
        setContentView(view)
        getSystemService(NotificationManager::class.java).cancel(ActionRunner.FLASH_NOTIFICATION_ID)
        handler.post(step)
    }

    override fun onDestroy() {
        handler.removeCallbacks(step)
        super.onDestroy()
    }

    private companion object {
        const val FRAMES = 6
        const val FRAME_MS = 250L
    }
}

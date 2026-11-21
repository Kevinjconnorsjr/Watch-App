package com.example.wristgestures.action

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.wristgestures.R
import com.example.wristgestures.service.GestureEvents
import com.example.wristgestures.ui.FlashActivity

/** Performs a [WatchAction]: plays a sound, flashes the screen or vibrates. */
class ActionRunner(private val context: Context) {

    // Media volume, so the watch's volume buttons/settings control it.
    private val tones: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
    }.getOrNull()

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun run(action: WatchAction) {
        when (action) {
            WatchAction.NONE -> Unit
            WatchAction.BEEP -> beep()
            WatchAction.CHIME -> chime()
            WatchAction.FLASH -> flash()
            WatchAction.VIBRATE -> vibrate()
        }
    }

    fun release() {
        tones?.release()
    }

    private fun beep() {
        tones?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
    }

    /** Plays the watch's notification sound, or a beep if it has none. */
    private fun chime() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = uri?.let { RingtoneManager.getRingtone(context, it) }
        if (ringtone != null) ringtone.play() else tones?.startTone(ToneGenerator.TONE_CDMA_PIP, 400)
    }

    private fun vibrate() {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80, 60, 250), -1))
    }

    private fun flash() {
        val intent = Intent(context, FlashActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)

        // Android only lets an app open a screen directly while the app is on screen.
        if (GestureEvents.uiVisible) {
            context.startActivity(intent)
            return
        }

        // Otherwise, a full-screen notification wakes the display and opens the flash.
        val notifications = context.getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(FLASH_CHANNEL, "Screen flash", NotificationManager.IMPORTANCE_HIGH)
        )
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, FLASH_CHANNEL)
            .setSmallIcon(R.drawable.ic_gesture)
            .setContentTitle("Flash")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pending, true)
            .setAutoCancel(true)
            .setTimeoutAfter(3000)
            .build()
        try {
            notifications.notify(FLASH_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w("ActionRunner", "Notifications not allowed, so the screen can't flash in the background", e)
        }
    }

    companion object {
        const val FLASH_CHANNEL = "flash"
        const val FLASH_NOTIFICATION_ID = 2
    }
}

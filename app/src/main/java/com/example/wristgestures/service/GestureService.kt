package com.example.wristgestures.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.wristgestures.R
import com.example.wristgestures.action.ActionRunner
import com.example.wristgestures.gesture.DetectorConfig
import com.example.wristgestures.gesture.Gesture
import com.example.wristgestures.gesture.GestureDetector
import com.example.wristgestures.gesture.GravityFilter
import com.example.wristgestures.settings.GestureSettings
import com.example.wristgestures.ui.MainActivity

/**
 * Listens to the motion sensors in the background (as a foreground service with an
 * ongoing notification) and runs the chosen action whenever a gesture is detected.
 */
class GestureService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var settings: GestureSettings
    private lateinit var actions: ActionRunner
    private lateinit var detector: GestureDetector
    private var wakeLock: PowerManager.WakeLock? = null
    private val gravityFilter = GravityFilter()
    private var listening = false

    private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == GestureSettings.KEY_SENSITIVITY) detector.config = currentConfig()
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SensorManager::class.java)
        settings = GestureSettings(this)
        actions = ActionRunner(this)
        detector = GestureDetector(currentConfig(), ::onGesture)
        settings.prefs.registerOnSharedPreferenceChangeListener(settingsListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        if (!listening) startListening()
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        listening = false
        wakeLock?.takeIf { it.isHeld }?.release()
        settings.prefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
        actions.release()
        GestureEvents.running.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        val timeMs = event.timestamp / 1_000_000
        val v = event.values
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> detector.onLinearAcceleration(timeMs, v[0], v[1], v[2])
            Sensor.TYPE_ACCELEROMETER -> {
                val linear = gravityFilter.linear(v[0], v[1], v[2])
                detector.onLinearAcceleration(timeMs, linear[0], linear[1], linear[2])
            }
            Sensor.TYPE_GYROSCOPE -> detector.onGyroscope(timeMs, v[0], v[1], v[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onGesture(gesture: Gesture) {
        GestureEvents.lastGesture.value = DetectedGesture(gesture, System.currentTimeMillis())
        actions.run(settings.actionFor(gesture))
    }

    private fun currentConfig() = DetectorConfig().scaled(settings.sensitivity.factor)

    private fun startListening() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyro == null) Log.w(TAG, "No gyroscope: only Shake will work")

        // About 50 readings per second, delivered immediately (no batching).
        listOfNotNull(accel, gyro).forEach {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Keeps the processor awake so gestures still work with the screen off.
        // This costs battery: expect noticeably faster drain while listening.
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WristGestures:listening")
            .apply { acquire() }

        listening = true
        GestureEvents.running.value = true
    }

    private fun startInForeground() {
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL, "Gesture listening", NotificationManager.IMPORTANCE_LOW)
        )
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, stopIntent(this), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_gesture)
            .setContentTitle("Listening for wrist gestures")
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "GestureService"
        private const val CHANNEL = "listening"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.example.wristgestures.STOP"

        fun start(context: Context) =
            context.startForegroundService(Intent(context, GestureService::class.java))

        fun stop(context: Context) {
            context.stopService(Intent(context, GestureService::class.java))
        }

        private fun stopIntent(context: Context) =
            Intent(context, GestureService::class.java).setAction(ACTION_STOP)
    }
}

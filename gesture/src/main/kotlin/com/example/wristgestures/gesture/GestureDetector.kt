package com.example.wristgestures.gesture

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Turns a stream of motion-sensor readings into [Gesture]s.
 *
 * Feed it every reading from the linear-acceleration and gyroscope sensors, with
 * timestamps in milliseconds. [onGesture] is called when a gesture is recognised.
 *
 * How each gesture is recognised:
 * - SHAKE: several strong acceleration spikes close together.
 * - TWIST: a fast rotation around the forearm, followed by a fast rotation the
 *   other way (turning the wrist over and back).
 * - FLICK: one fast rotation around the watch's x axis, as when you snap your hand
 *   up. It is only reported after a short wait, so a shake isn't mistaken for a flick.
 */
class GestureDetector(
    config: DetectorConfig = DetectorConfig(),
    private val onGesture: (Gesture) -> Unit,
) {
    var config: DetectorConfig = config
        set(value) {
            field = value
            reset()
        }

    private var cooldownUntil = NEVER

    // Shake state
    private val strokeTimes = ArrayDeque<Long>()
    private var accelArmed = true

    // Twist state
    private var twistArmed = true
    private var twistHalfSign = 0
    private var twistHalfTime = NEVER

    // Flick state
    private var flickArmed = true
    private var flickCandidateTime: Long? = null

    fun onLinearAcceleration(timeMs: Long, x: Float, y: Float, z: Float) {
        confirmPendingFlick(timeMs)
        if (timeMs < cooldownUntil) return

        val magnitude = sqrt(x * x + y * y + z * z)
        if (accelArmed && magnitude > config.shakeAccel) {
            accelArmed = false
            strokeTimes.addLast(timeMs)
            while (timeMs - strokeTimes.first() > config.shakeWindowMs) strokeTimes.removeFirst()
            // A flick causes a spike or two; three or more means a shake is under way.
            if (strokeTimes.size >= 3) flickCandidateTime = null
            if (strokeTimes.size >= config.shakeStrokes) emit(Gesture.SHAKE, timeMs)
        } else if (!accelArmed && magnitude < config.shakeAccel * REARM) {
            accelArmed = true
        }
    }

    fun onGyroscope(timeMs: Long, x: Float, y: Float, z: Float) {
        confirmPendingFlick(timeMs)
        if (timeMs < cooldownUntil) return

        val ax = abs(x)
        val ay = abs(y)
        val az = abs(z)

        // Twist: rotation around the forearm (y axis), first one way, then the other.
        if (twistArmed && ay > config.twistRate && ay > DOMINANCE * max(ax, az)) {
            twistArmed = false
            val direction = sign(y).toInt()
            if (twistHalfSign == -direction && timeMs - twistHalfTime <= config.twistWindowMs) {
                emit(Gesture.TWIST, timeMs)
                return
            }
            twistHalfSign = direction
            twistHalfTime = timeMs
            flickCandidateTime = null
        } else if (!twistArmed && ay < config.twistRate * REARM) {
            twistArmed = true
        }

        // Flick: one sharp rotation around the x axis, not part of a twist.
        if (flickArmed && ax > config.flickRate && ax > DOMINANCE * max(ay, az)) {
            flickArmed = false
            val recentTwist = timeMs - twistHalfTime <= config.twistWindowMs
            if (flickCandidateTime == null && !recentTwist) flickCandidateTime = timeMs
        } else if (!flickArmed && ax < config.flickRate * REARM) {
            flickArmed = true
        }
    }

    fun reset() {
        cooldownUntil = NEVER
        resetMotionState()
    }

    private fun confirmPendingFlick(timeMs: Long) {
        val candidate = flickCandidateTime ?: return
        if (timeMs - candidate >= config.flickConfirmMs) emit(Gesture.FLICK, timeMs)
    }

    private fun emit(gesture: Gesture, timeMs: Long) {
        cooldownUntil = timeMs + config.cooldownMs
        resetMotionState()
        onGesture(gesture)
    }

    private fun resetMotionState() {
        strokeTimes.clear()
        flickCandidateTime = null
        twistHalfSign = 0
        twistHalfTime = NEVER
        // Stay disarmed until the motion settles, so the tail of a gesture isn't counted.
        accelArmed = false
        twistArmed = false
        flickArmed = false
    }

    private companion object {
        /** The main axis must be this many times stronger than the others. */
        const val DOMINANCE = 1.5f
        /** A spike ends once the signal drops below this fraction of the threshold. */
        const val REARM = 0.5f
        const val NEVER = Long.MIN_VALUE / 2
    }
}

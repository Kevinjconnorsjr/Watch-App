package com.example.wristgestures.gesture

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Feeds synthetic sensor traces (50 readings per second, like the real watch) into the
 * detector and checks what it reports.
 */
class GestureDetectorTest {

    private data class Sample(
        val ax: Float = 0f, val ay: Float = 0f, val az: Float = 0f,
        val gx: Float = 0f, val gy: Float = 0f, val gz: Float = 0f,
    )

    /** Runs [motion] for [durationMs], then [quietMs] of stillness, and returns what fired. */
    private fun detect(
        durationMs: Long,
        quietMs: Long = 1000,
        config: DetectorConfig = DetectorConfig(),
        motion: (tSeconds: Double) -> Sample,
    ): List<Gesture> {
        val found = mutableListOf<Gesture>()
        val detector = GestureDetector(config) { found += it }
        var t = 0L
        while (t <= durationMs + quietMs) {
            val s = if (t <= durationMs) motion(t / 1000.0) else Sample()
            detector.onLinearAcceleration(t, s.ax, s.ay, s.az)
            detector.onGyroscope(t, s.gx, s.gy, s.gz)
            t += 20
        }
        return found
    }

    private fun pulse(t: Double, center: Double, height: Float, width: Double = 0.04): Float =
        (height * exp(-((t - center) * (t - center)) / (2 * width * width))).toFloat()

    @Test
    fun shakeIsDetected() {
        val found = detect(1000) { t ->
            Sample(ax = (20 * sin(2 * PI * 3 * t)).toFloat(), gz = (2 * sin(2 * PI * 3 * t)).toFloat())
        }
        assertEquals(listOf(Gesture.SHAKE), found)
    }

    @Test
    fun twistOverAndBackIsDetected() {
        // One full cycle: turn one way, then back.
        val found = detect(660) { t -> Sample(gy = (8 * sin(2 * PI * 1.5 * t)).toFloat(), gx = 1f) }
        assertEquals(listOf(Gesture.TWIST), found)
    }

    @Test
    fun turningWristOnlyOneWayIsNotATwist() {
        val found = detect(330) { t -> Sample(gy = (8 * sin(2 * PI * 1.5 * t)).toFloat()) }
        assertEquals(emptyList(), found)
    }

    @Test
    fun flickIsDetected() {
        val found = detect(500) { t ->
            Sample(
                gx = pulse(t, 0.2, 9f) - pulse(t, 0.35, 6f), // snap up, then settle back
                ay = pulse(t, 0.2, 16f),
            )
        }
        assertEquals(listOf(Gesture.FLICK), found)
    }

    @Test
    fun gentleArmMovementTriggersNothing() {
        val found = detect(3000) { t ->
            Sample(
                ax = (3 * sin(2 * PI * 0.7 * t)).toFloat(),
                ay = (2 * sin(2 * PI * 1.1 * t)).toFloat(),
                gx = (1.5 * sin(2 * PI * 0.5 * t)).toFloat(),
                gy = (2 * sin(2 * PI * 0.9 * t)).toFloat(),
            )
        }
        assertEquals(emptyList(), found)
    }

    @Test
    fun higherSensitivityCatchesGentlerShake() {
        val gentleShake = { t: Double -> Sample(ax = (12 * sin(2 * PI * 3 * t)).toFloat()) }
        assertEquals(emptyList(), detect(1000, motion = gentleShake))
        assertEquals(
            listOf(Gesture.SHAKE),
            detect(1000, config = DetectorConfig().scaled(1.3f), motion = gentleShake),
        )
    }

    @Test
    fun twoSeparateGesturesBothFire() {
        val found = detect(2600) { t ->
            if (t < 1.0) Sample(ax = (20 * sin(2 * PI * 3 * t)).toFloat())
            else if (t in 1.9..2.56) Sample(gy = (8 * sin(2 * PI * 1.5 * (t - 1.9))).toFloat())
            else Sample()
        }
        assertEquals(listOf(Gesture.SHAKE, Gesture.TWIST), found)
    }

    @Test
    fun gravityFilterRemovesConstantGravity() {
        val filter = GravityFilter()
        var last = floatArrayOf()
        repeat(100) { last = filter.linear(0f, 0f, 9.81f) }
        assertEquals(0f, last[2], 0.01f)
    }
}

package com.example.wristgestures.gesture

/**
 * Thresholds for [GestureDetector]. Watch axes when worn: x points toward 3 o'clock,
 * y toward 12 o'clock (along your forearm), z out of the screen.
 */
data class DetectorConfig(
    /** Linear acceleration (m/s², gravity removed) that counts as one shake stroke. */
    val shakeAccel: Float = 14f,
    /** Strokes needed within [shakeWindowMs]. Each back-and-forth is about 2 strokes. */
    val shakeStrokes: Int = 4,
    val shakeWindowMs: Long = 1200,

    /** Rotation speed (rad/s) around the forearm (y axis) that counts as half a twist. */
    val twistRate: Float = 5f,
    /** Both halves of a twist (over and back) must happen within this time. */
    val twistWindowMs: Long = 700,

    /** Rotation speed (rad/s) around the x axis that counts as a flick. */
    val flickRate: Float = 6f,
    /** Wait this long before reporting a flick, to make sure it isn't the start of a shake. */
    val flickConfirmMs: Long = 300,

    /** After any gesture, ignore motion for this long so one movement fires once. */
    val cooldownMs: Long = 900,
) {
    /** Higher [sensitivity] lowers the thresholds, so gentler motions count. */
    fun scaled(sensitivity: Float): DetectorConfig = copy(
        shakeAccel = shakeAccel / sensitivity,
        twistRate = twistRate / sensitivity,
        flickRate = flickRate / sensitivity,
    )
}

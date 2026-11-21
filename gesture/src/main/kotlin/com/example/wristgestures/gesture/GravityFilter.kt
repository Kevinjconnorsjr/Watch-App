package com.example.wristgestures.gesture

/**
 * Removes gravity from raw accelerometer readings with a low-pass filter. Only needed
 * on watches without a linear-acceleration sensor.
 */
class GravityFilter(private val alpha: Float = 0.8f) {
    private val gravity = FloatArray(3)
    private var initialised = false

    /** Returns the acceleration with gravity removed. */
    fun linear(x: Float, y: Float, z: Float): FloatArray {
        if (!initialised) {
            gravity[0] = x; gravity[1] = y; gravity[2] = z
            initialised = true
        } else {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * x
            gravity[1] = alpha * gravity[1] + (1 - alpha) * y
            gravity[2] = alpha * gravity[2] + (1 - alpha) * z
        }
        return floatArrayOf(x - gravity[0], y - gravity[1], z - gravity[2])
    }
}

package com.example.wristgestures.gesture

/** The wrist motions the app recognises. Add a new entry here to add a gesture. */
enum class Gesture(val label: String, val howTo: String) {
    FLICK("Flick", "Snap your hand up quickly"),
    SHAKE("Shake", "Shake your wrist back and forth"),
    TWIST("Twist", "Turn your wrist over and back"),
}

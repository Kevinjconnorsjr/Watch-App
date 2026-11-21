package com.example.wristgestures.action

/** Things the watch can do when a gesture fires. Add a new entry here (and handle it in ActionRunner) to add an action. */
enum class WatchAction(val label: String) {
    NONE("Do nothing"),
    BEEP("Beep"),
    CHIME("Chime"),
    FLASH("Flash screen"),
    VIBRATE("Vibrate");

    fun next(): WatchAction = entries[(ordinal + 1) % entries.size]
}

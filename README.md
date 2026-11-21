# Wrist Gestures

A Wear OS app for the Samsung Galaxy Watch6 Classic (it works on any Wear OS 3+ watch).
It recognizes your own wrist motions and does something for each one: plays a sound,
flashes the screen or vibrates.

| Gesture | How to do it | Default action |
|---|---|---|
| Flick | Snap your hand up quickly | Beep |
| Shake | Shake your wrist back and forth | Flash screen |
| Twist | Turn your wrist over and back | Vibrate |

On the watch, tap a gesture to change its action. The watch plays each choice as you
pick it. The options are Do nothing, Beep, Chime (the watch's notification sound),
Flash screen and Vibrate. Set **Sensitivity** to High if gestures are hard to trigger,
or Low if they fire by accident. The top button shows the last gesture it detected,
which helps when you're practicing.

## Install it on your watch

1. Install [Android Studio](https://developer.android.com/studio) and open this folder.
2. On the watch, turn on developer mode: **Settings → About watch → Software
   information**, then tap **Software version** 5 times.
3. In **Settings → Developer options**, turn on **ADB debugging** and **Wireless
   debugging**. The watch and computer must be on the same Wi-Fi network.
4. In Android Studio, open the device menu, choose **Pair Devices Using Wi-Fi**, and
   enter the pairing code shown on the watch under Wireless debugging → Pair new device.
5. Select the watch and press **Run**. The app shows up in the watch's app list as
   **Wrist Gestures**.

## Good to know

- **Battery:** while listening, the app keeps the motion sensors and processor on so
  gestures work with the screen off. This drains the battery noticeably faster, so tap
  **Stop listening** (or Stop in the notification) when you don't need it.
- **Sounds** use the watch's media volume. If you can't hear anything, raise the volume.
- **Screen flash** opens straight away while the app is on screen. In the background it
  uses a full-screen notification, so allow notifications when the app asks. Flashes
  are kept to 2 per second, below the rate that can trigger photosensitive seizures.
- Samsung's built-in gestures (Settings → Accessibility → Interaction and dexterity →
  Universal gestures, and the double-pinch gesture) are separate from this app, and only
  Samsung can add to those.

## How to add your own gesture or action

- **New action:** add an entry to `WatchAction` in
  `app/src/main/java/com/example/wristgestures/action/WatchAction.kt`, then handle it in
  `ActionRunner.run()`.
- **New gesture:** add an entry to `Gesture` in `gesture/src/main/kotlin/.../Gesture.kt`,
  detect it in `GestureDetector`, and give it a default in `GestureSettings.defaultAction`.
- **Tuning:** thresholds live in `DetectorConfig`. The detector is plain Kotlin with
  tests that use simulated sensor data: `./gradlew :gesture:test`.

## Project layout

- `gesture/`: gesture recognition, plain Kotlin, unit tested
- `app/.../service/GestureService.kt`: background sensor listening
- `app/.../action/ActionRunner.kt`: sounds, vibration, screen flash
- `app/.../ui/`: watch screens (settings list and the flash screen)

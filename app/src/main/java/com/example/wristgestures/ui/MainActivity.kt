package com.example.wristgestures.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.wristgestures.action.ActionRunner
import com.example.wristgestures.action.WatchAction
import com.example.wristgestures.gesture.Gesture
import com.example.wristgestures.service.GestureEvents
import com.example.wristgestures.service.GestureService
import com.example.wristgestures.settings.GestureSettings

class MainActivity : ComponentActivity() {

    private lateinit var settings: GestureSettings
    private lateinit var preview: ActionRunner

    // Notifications are needed for the "listening" notice and for flashing in the background.
    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            GestureService.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = GestureSettings(this)
        preview = ActionRunner(this)
        setContent {
            GestureScreen(
                settings = settings,
                onStart = ::startListening,
                onStop = { GestureService.stop(this) },
                onPreview = preview::run,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        GestureEvents.uiVisible = true
    }

    override fun onPause() {
        GestureEvents.uiVisible = false
        super.onPause()
    }

    override fun onDestroy() {
        preview.release()
        super.onDestroy()
    }

    private fun startListening() {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsk) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GestureService.start(this)
        }
    }
}

@Composable
private fun GestureScreen(
    settings: GestureSettings,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPreview: (WatchAction) -> Unit,
) {
    val running by GestureEvents.running.collectAsState()
    val last by GestureEvents.lastGesture.collectAsState()
    var actions by remember { mutableStateOf(Gesture.entries.associateWith(settings::actionFor)) }
    var sensitivity by remember { mutableStateOf(settings.sensitivity) }
    val listState = rememberScalingLazyListState()

    MaterialTheme {
        Scaffold(
            timeText = { TimeText() },
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        ) {
            ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item { ListHeader { Text("Wrist Gestures") } }

                item {
                    Chip(
                        onClick = if (running) onStop else onStart,
                        label = { Text(if (running) "Stop listening" else "Start listening") },
                        secondaryLabel = {
                            Text(
                                when {
                                    !running -> "Off"
                                    last == null -> "Waiting for a gesture"
                                    else -> "Last: ${last!!.gesture.label}"
                                }
                            )
                        },
                        colors = if (running) ChipDefaults.secondaryChipColors() else ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Gesture.entries.forEach { gesture ->
                    item {
                        Chip(
                            onClick = {
                                val next = actions.getValue(gesture).next()
                                settings.setAction(gesture, next)
                                actions = actions + (gesture to next)
                                onPreview(next)
                            },
                            label = { Text(gesture.label) },
                            secondaryLabel = { Text(actions.getValue(gesture).label) },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    Chip(
                        onClick = {
                            sensitivity = sensitivity.next()
                            settings.sensitivity = sensitivity
                        },
                        label = { Text("Sensitivity") },
                        secondaryLabel = { Text(sensitivity.label) },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        "Tap a gesture to change its action.\n" +
                            Gesture.entries.joinToString("\n") { "${it.label}: ${it.howTo}" },
                        style = MaterialTheme.typography.caption3,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

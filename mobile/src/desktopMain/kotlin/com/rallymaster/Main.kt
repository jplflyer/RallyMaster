package com.rallymaster

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rallymaster.ui.App

/**
 * Main entry point for the Rally Master Desktop Application.
 *
 * This desktop-first application focuses on local JSON storage for Rally Masters
 * to create and manage motorcycle rallies offline-first.
 */
fun main() = application {
    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Rally Master - Desktop",
        state = windowState,
    ) {
        App()
    }
}

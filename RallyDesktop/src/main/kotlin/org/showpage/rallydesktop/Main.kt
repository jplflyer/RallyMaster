package org.showpage.rallydesktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.showpage.rallydesktop.model.AppState
import org.showpage.rallydesktop.model.Screen
import org.showpage.rallydesktop.service.CredentialService
import org.showpage.rallydesktop.service.PreferencesService
import org.showpage.rallydesktop.ui.RallyMasterApp
import org.slf4j.LoggerFactory
import java.awt.event.KeyEvent

private val logger = LoggerFactory.getLogger("RallyMaster")

fun main() {
    // Set the app name for macOS menu bar and Dock - must be before application()
    System.setProperty("apple.awt.application.name", "RallyMaster")
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "RallyMaster")

    // Set User-Agent for HTTP requests (needed for OpenStreetMap tiles)
    System.setProperty("http.agent", "RallyMaster/1.0")

    application {
    val preferencesService = remember { PreferencesService() }
    val credentialService = remember { CredentialService.create() }
    val appState = remember { AppState() }

    // Restore window position and size from preferences, or use defaults
    val initialWidth = preferencesService.getWindowWidth()?.dp ?: 1200.dp
    val initialHeight = preferencesService.getWindowHeight()?.dp ?: 800.dp
    val initialX = preferencesService.getWindowX()?.dp
    val initialY = preferencesService.getWindowY()?.dp

    val windowState = rememberWindowState(
        size = DpSize(initialWidth, initialHeight),
        position = if (initialX != null && initialY != null) {
            WindowPosition(initialX, initialY)
        } else {
            WindowPosition.Aligned(androidx.compose.ui.Alignment.Center)
        }
    )

    Window(
        onCloseRequest = {
            // Save window bounds before closing
            logger.info("Saving window bounds before exit")
            preferencesService.saveWindowBounds(
                x = windowState.position.x.value.toInt(),
                y = windowState.position.y.value.toInt(),
                width = windowState.size.width.value.toInt(),
                height = windowState.size.height.value.toInt()
            )
            exitApplication()
        },
        state = windowState,
        title = "RallyMaster",
        icon = painterResource("icon.png")
    ) {
        // Menu bar
        MenuBar {
            // File menu (or RallyMaster menu on macOS)
            Menu("File") {
                // Settings - only show when authenticated
                if (appState.isAuthenticated) {
                    Item(
                        "Settings...",
                        onClick = {
                            appState.navigateTo(Screen.SETTINGS)
                        },
                        shortcut = KeyShortcut(Key.Comma, meta = true) // Cmd+, on Mac, Ctrl+, on Windows
                    )
                    Separator()
                }

                // Logout - only show when authenticated
                if (appState.isAuthenticated) {
                    Item(
                        "Logout",
                        onClick = {
                            // Clear credentials and logout
                            val email = preferencesService.getEmail()
                            if (email != null) {
                                credentialService.deletePassword("RallyMaster", email)
                            }
                            preferencesService.clear()
                            appState.clearAuthentication()
                        },
                        shortcut = KeyShortcut(Key.L, meta = true, shift = true) // Cmd+Shift+L / Ctrl+Shift+L
                    )
                    Separator()
                }

                Item(
                    "Quit",
                    onClick = { exitApplication() },
                    shortcut = KeyShortcut(Key.Q, meta = true) // Cmd+Q on Mac, Ctrl+Q on Windows
                )
            }
        }
        // Monitor window position and size changes and save them
        LaunchedEffect(windowState) {
            snapshotFlow { windowState.position }
                .onEach { position ->
                    if (position is WindowPosition.Absolute) {
                        logger.debug("Window moved to: x={}, y={}", position.x, position.y)
                    }
                }
                .launchIn(this)

            snapshotFlow { windowState.size }
                .onEach { size ->
                    logger.debug("Window resized to: width={}, height={}", size.width, size.height)
                }
                .launchIn(this)
        }

        RallyMasterApp(
            appState = appState,
            preferencesService = preferencesService,
            credentialService = credentialService
        )
    }
    }
}

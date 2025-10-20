package org.showpage.rallydesktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import org.showpage.rallydesktop.ui.RallyMasterApp
import org.showpage.rallydesktop.service.PreferencesService
import org.showpage.rallydesktop.service.CredentialService
import org.showpage.rallydesktop.service.RallyServerClient
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RallyMaster")

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "RallyMaster Desktop"
    ) {
        RallyMasterApp()
    }
}

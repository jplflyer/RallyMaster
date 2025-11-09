package org.showpage.rallydesktop.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.showpage.rallyserver.ui.UiMember

/**
 * Application state holder.
 * Manages the current state of the application including authentication and navigation.
 */
class AppState {
    // Splash screen state
    var showSplash by mutableStateOf(true)
        private set

    // Authentication state
    var isAuthenticated by mutableStateOf(false)
        private set

    var currentUser by mutableStateOf<UiMember?>(null)
        private set

    // Loading state
    var isLoading by mutableStateOf(false)
        private set

    var loadingMessage by mutableStateOf<String?>(null)
        private set

    // Error state
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Screen navigation
    var currentScreen by mutableStateOf(Screen.SPLASH)
        private set

    var currentRallyId by mutableStateOf<Int?>(null)
        private set

    fun hideSplash() {
        showSplash = false
        currentScreen = if (isAuthenticated) Screen.HOME else Screen.LOGIN
    }

    fun setAuthenticated(user: UiMember) {
        isAuthenticated = true
        currentUser = user
        currentScreen = Screen.HOME
    }

    fun clearAuthentication() {
        isAuthenticated = false
        currentUser = null
        currentScreen = Screen.LOGIN
    }

    fun setLoading(loading: Boolean, message: String? = null) {
        isLoading = loading
        loadingMessage = message
    }

    fun setError(message: String?) {
        errorMessage = message
    }

    fun clearError() {
        errorMessage = null
    }

    fun navigateTo(screen: Screen) {
        currentScreen = screen
        // Clear rallyId when navigating away from rally-specific screens
        if (screen != Screen.RALLY_PLANNING && screen != Screen.RALLY_FORM) {
            currentRallyId = null
        }
    }

    fun navigateToRallyPlanning(rallyId: Int) {
        currentRallyId = rallyId
        currentScreen = Screen.RALLY_PLANNING
    }

    fun navigateToRallyForm(rallyId: Int? = null) {
        currentRallyId = rallyId
        currentScreen = Screen.RALLY_FORM
    }
}

enum class Screen {
    SPLASH,
    LOGIN,
    HOME,
    RALLY_FORM,        // Create/edit rally form
    RALLY_PLANNING,    // Rally planning workspace
    RIDE_PLANNING,
    SCORING,
    SETTINGS
}

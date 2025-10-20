package org.showpage.rallydesktop.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.showpage.rallydesktop.model.AppState
import org.showpage.rallydesktop.model.Screen
import org.showpage.rallydesktop.service.CredentialService
import org.showpage.rallydesktop.service.PreferencesService
import org.showpage.rallydesktop.service.RallyServerClient
import org.slf4j.LoggerFactory

/**
 * Main application component that manages the application state and navigation.
 */
@Composable
fun RallyMasterApp() {
    val logger = LoggerFactory.getLogger("RallyMasterApp")
    val appState = remember { AppState() }
    val preferencesService = remember { PreferencesService() }
    val credentialService = remember { CredentialService.create() }
    val scope = rememberCoroutineScope()

    // Initialize server client with stored or default URL
    var serverClient by remember {
        mutableStateOf(RallyServerClient(preferencesService.getServerUrl()))
    }

    // Splash screen logic - show for minimum 3 seconds and attempt auto-login
    LaunchedEffect(Unit) {
        logger.info("Application starting...")
        val startTime = System.currentTimeMillis()

        // Check for stored credentials and attempt auto-login
        val storedEmail = preferencesService.getEmail()
        val storedPassword = storedEmail?.let { credentialService.getPassword("RallyMaster", it) }

        if (storedEmail != null && storedPassword != null) {
            logger.info("Found stored credentials, attempting auto-login for: {}", storedEmail)
            appState.setLoading(true, "Logging in...")

            serverClient.login(storedEmail, storedPassword).fold(
                onSuccess = { authResponse ->
                    logger.info("Auto-login successful")
                    // Get user info
                    serverClient.getMemberInfo().fold(
                        onSuccess = { member ->
                            appState.setAuthenticated(member)
                        },
                        onFailure = { error ->
                            logger.error("Failed to get member info: {}", error.message)
                            appState.setError("Failed to get user information")
                        }
                    )
                },
                onFailure = { error ->
                    logger.warn("Auto-login failed: {}", error.message)
                    appState.setError("Auto-login failed. Please login again.")
                    // Clear invalid credentials
                    credentialService.deletePassword("RallyMaster", storedEmail)
                }
            )

            appState.setLoading(false)
        } else {
            logger.info("No stored credentials found")
        }

        // Ensure splash screen shows for at least 3 seconds
        val elapsed = System.currentTimeMillis() - startTime
        val minimumSplashTime = 3000L
        val remainingTime = minimumSplashTime - elapsed

        logger.info("Splash screen: elapsed={}ms, remaining={}ms", elapsed, remainingTime)

        if (remainingTime > 0) {
            logger.info("Delaying for {}ms to meet minimum splash time", remainingTime)
            delay(remainingTime)
        }

        logger.info("Hiding splash screen")
        appState.hideSplash()
    }

    MaterialTheme {
        when (appState.currentScreen) {
            Screen.SPLASH -> {
                SplashScreen(
                    statusMessage = appState.loadingMessage ?: "Loading..."
                )
            }

            Screen.LOGIN -> {
                LoginScreen(
                    onLogin = { email, password ->
                        scope.launch {
                            appState.setLoading(true, "Logging in...")
                            appState.clearError()

                            serverClient.login(email, password).fold(
                                onSuccess = { authResponse ->
                                    // Store credentials
                                    preferencesService.setEmail(email)
                                    credentialService.storePassword("RallyMaster", email, password)

                                    // Get user info
                                    serverClient.getMemberInfo().fold(
                                        onSuccess = { member ->
                                            appState.setAuthenticated(member)
                                        },
                                        onFailure = { error ->
                                            appState.setError("Failed to get user information: ${error.message}")
                                        }
                                    )
                                },
                                onFailure = { error ->
                                    appState.setError("Login failed: ${error.message}")
                                }
                            )

                            appState.setLoading(false)
                        }
                    },
                    onRegister = { email, password ->
                        scope.launch {
                            appState.setLoading(true, "Registering...")
                            appState.clearError()

                            serverClient.register(email, password).fold(
                                onSuccess = { member ->
                                    // After registration, automatically log in
                                    serverClient.login(email, password).fold(
                                        onSuccess = {
                                            preferencesService.setEmail(email)
                                            credentialService.storePassword("RallyMaster", email, password)
                                            appState.setAuthenticated(member)
                                        },
                                        onFailure = { error ->
                                            appState.setError("Registration successful, but login failed: ${error.message}")
                                        }
                                    )
                                },
                                onFailure = { error ->
                                    appState.setError("Registration failed: ${error.message}")
                                }
                            )

                            appState.setLoading(false)
                        }
                    },
                    errorMessage = appState.errorMessage,
                    isLoading = appState.isLoading
                )
            }

            Screen.HOME -> {
                appState.currentUser?.let { user ->
                    HomeScreen(
                        user = user,
                        onLogout = {
                            // Clear credentials and logout
                            val email = preferencesService.getEmail()
                            if (email != null) {
                                credentialService.deletePassword("RallyMaster", email)
                            }
                            preferencesService.clear()
                            serverClient.logout()
                            appState.clearAuthentication()
                        },
                        onNavigateToRallyPlanning = {
                            // TODO: Implement Rally Planning screen
                            appState.navigateTo(Screen.RALLY_PLANNING)
                        },
                        onNavigateToRidePlanning = {
                            // TODO: Implement Ride Planning screen
                            appState.navigateTo(Screen.RIDE_PLANNING)
                        },
                        onNavigateToScoring = {
                            // TODO: Implement Scoring screen
                            appState.navigateTo(Screen.SCORING)
                        },
                        onNavigateToSettings = {
                            appState.navigateTo(Screen.SETTINGS)
                        }
                    )
                }
            }

            Screen.SETTINGS -> {
                SettingsScreen(
                    currentServerUrl = preferencesService.getServerUrl(),
                    onServerUrlChange = { newUrl ->
                        preferencesService.setServerUrl(newUrl)
                        // Recreate server client with new URL
                        serverClient = RallyServerClient(newUrl)
                    },
                    onBack = {
                        appState.navigateTo(Screen.HOME)
                    }
                )
            }

            Screen.RALLY_PLANNING -> {
                // TODO: Implement Rally Planning screen
                PlaceholderScreen("Rally Planning (Coming Soon)", onBack = {
                    appState.navigateTo(Screen.HOME)
                })
            }

            Screen.RIDE_PLANNING -> {
                // TODO: Implement Ride Planning screen
                PlaceholderScreen("Ride Planning (Coming Soon)", onBack = {
                    appState.navigateTo(Screen.HOME)
                })
            }

            Screen.SCORING -> {
                // TODO: Implement Scoring screen
                PlaceholderScreen("Scoring (Coming Soon)", onBack = {
                    appState.navigateTo(Screen.HOME)
                })
            }
        }
    }
}

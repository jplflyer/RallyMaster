package org.showpage.rallydesktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Login screen for user authentication.
 */
@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (email: String, password: String) -> Unit,
    errorMessage: String? = null,
    isLoading: Boolean = false
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    // Helper function to handle login/register action
    val performAction = {
        if (email.isNotBlank() && password.isNotBlank() && !isLoading) {
            if (isRegisterMode) {
                onRegister(email, password)
            } else {
                onLogin(email, password)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isRegisterMode) "Register" else "Login",
                    style = MaterialTheme.typography.headlineMedium
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    enabled = !isLoading,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                performAction()
                                true
                            } else {
                                false
                            }
                        }
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isLoading,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                performAction()
                                true
                            } else {
                                false
                            }
                        }
                )

                Button(
                    onClick = performAction,
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isRegisterMode) "Register" else "Login")
                    }
                }

                TextButton(
                    onClick = { isRegisterMode = !isRegisterMode },
                    enabled = !isLoading
                ) {
                    Text(
                        text = if (isRegisterMode) {
                            "Already have an account? Login"
                        } else {
                            "Don't have an account? Register"
                        }
                    )
                }
            }
        }
    }
}

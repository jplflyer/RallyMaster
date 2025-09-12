package com.rallymaster.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

/**
 * Main application composable for Rally Master Desktop.
 *
 * This is the root of the UI hierarchy, managing navigation between different screens
 * for rally creation, editing, and management using local JSON storage.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface {
            MainScreen()
        }
    }
}

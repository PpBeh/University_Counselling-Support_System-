// Entry point | Owner: shared (Beh, Jeremy, Michael)
package com.example.counselling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.counselling.core.settings.ThemeMode
import com.example.counselling.navigation.AppRoot
import com.example.counselling.ui.theme.CounsellingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as CounsellingApp).container

        setContent {
            val themeMode by container.themePreferences.mode.collectAsState()
            CounsellingTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
            ) {
                AppRoot(container = container)
            }
        }
    }
}

// Entry point | Owner: shared (Beh, Jeremy, Michael)
package com.example.counselling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.counselling.navigation.AppRoot
import com.example.counselling.ui.theme.CounsellingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CounsellingTheme {
                val app = application as CounsellingApp
                AppRoot(container = app.container)
            }
        }
    }
}

// Core settings | Owner: shared (Beh, Jeremy, Michael)
package com.example.counselling.core.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    ;

    companion object {
        fun fromLabel(label: String): ThemeMode =
            entries.firstOrNull { it.label == label } ?: SYSTEM
    }
}

// OOP: Encapsulation - the selected appearance is persisted and exposed as a
// read-only StateFlow; UI code cannot mutate the preference directly.
class ThemePreferences(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences("appearance", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(readMode())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        if (_mode.value == mode) return
        preferences.edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }

    private fun readMode(): ThemeMode {
        val stored = preferences.getString(KEY_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private companion object {
        const val KEY_MODE = "theme_mode"
    }
}

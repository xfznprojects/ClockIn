package com.clockin.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("clockin_prefs", Context.MODE_PRIVATE)

    // Dark mode
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }

    // Prevent duplicate entries
    private val _preventDuplicates = MutableStateFlow(prefs.getBoolean(KEY_PREVENT_DUPLICATES, true))
    val preventDuplicates: StateFlow<Boolean> = _preventDuplicates.asStateFlow()

    fun setPreventDuplicates(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PREVENT_DUPLICATES, enabled).apply()
        _preventDuplicates.value = enabled
    }

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_PREVENT_DUPLICATES = "prevent_duplicates"
    }
}

package com.example.digitallibrary.ui.common

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    MARATHI("mr", "Marathi", "मराठी")
}

object LanguageManager {
    private const val PREFS_NAME = "digital_library_settings"
    private const val KEY_LANGUAGE = "selected_language"

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.code)
        _currentLanguage.value = if (savedCode == AppLanguage.MARATHI.code) AppLanguage.MARATHI else AppLanguage.ENGLISH
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        _currentLanguage.value = language
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }
}

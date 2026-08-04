package com.example.mynote.data

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ViewMode { GRID, LIST }
enum class SortOrder { DATE_MODIFIED, DATE_CREATED, TITLE_ASC }

/**
 * Tiny SharedPreferences-backed store for the handful of settings the app
 * remembers between launches (theme, default notes view, sort order).
 */
class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var viewMode: ViewMode
        get() = runCatching {
            ViewMode.valueOf(prefs.getString(KEY_VIEW, ViewMode.GRID.name)!!)
        }.getOrDefault(ViewMode.GRID)
        set(value) = prefs.edit().putString(KEY_VIEW, value.name).apply()

    var sortOrder: SortOrder
        get() = runCatching {
            SortOrder.valueOf(prefs.getString(KEY_SORT, SortOrder.DATE_MODIFIED.name)!!)
        }.getOrDefault(SortOrder.DATE_MODIFIED)
        set(value) = prefs.edit().putString(KEY_SORT, value.name).apply()

    companion object {
        private const val PREFS_NAME = "mynote_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_VIEW = "view_mode"
        private const val KEY_SORT = "sort_order"
    }
}

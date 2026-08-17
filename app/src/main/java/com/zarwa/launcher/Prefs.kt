package com.zarwa.launcher

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Small persistent settings store. */
object Prefs {
    private const val FILE = "zarwa_prefs"
    private const val KEY_DARK = "dark_mode"
    private const val KEY_LAT = "lat"
    private const val KEY_LON = "lon"
    private const val KEY_CITY = "city"
    private const val KEY_BG = "bg_uri"
    private const val KEY_GRID = "grid_config"
    private const val KEY_THEME = "theme_index"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Dark by default (the premium look). */
    fun isDark(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_DARK, true)

    fun setDark(ctx: Context, dark: Boolean) {
        sp(ctx).edit().putBoolean(KEY_DARK, dark).apply()
        applyTheme(dark)
    }

    fun applyTheme(dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // Weather location — defaults to Cairo, editable later.
    fun lat(ctx: Context): Double = sp(ctx).getFloat(KEY_LAT, 30.06f).toDouble()
    fun lon(ctx: Context): Double = sp(ctx).getFloat(KEY_LON, 31.25f).toDouble()
    fun city(ctx: Context): String = sp(ctx).getString(KEY_CITY, "القاهرة") ?: "القاهرة"

    fun setLocation(ctx: Context, lat: Double, lon: Double, city: String) {
        sp(ctx).edit()
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LON, lon.toFloat())
            .putString(KEY_CITY, city)
            .apply()
    }

    // Custom home-screen background image (content:// URI), null = use the gradient.
    fun bgUri(ctx: Context): String? = sp(ctx).getString(KEY_BG, null)

    fun setBgUri(ctx: Context, uri: String?) {
        sp(ctx).edit().putString(KEY_BG, uri).apply()
    }

    // Customizable grid config, stored as JSON. Empty = first-run default.
    fun gridConfig(ctx: Context): String = sp(ctx).getString(KEY_GRID, "") ?: ""

    fun setGridConfig(ctx: Context, json: String) {
        sp(ctx).edit().putString(KEY_GRID, json).apply()
    }

    // The user's name, shown in the greeting (empty = no name).
    fun userName(ctx: Context): String = sp(ctx).getString("user_name", "") ?: ""
    fun setUserName(ctx: Context, name: String) { sp(ctx).edit().putString("user_name", name).apply() }

    // Living background style: 0 aurora, 1 waves, 2 particles.
    fun bgStyle(ctx: Context): Int = sp(ctx).getInt("bg_style", 0)
    fun setBgStyle(ctx: Context, i: Int) { sp(ctx).edit().putInt("bg_style", i).apply() }

    // Colour theme preset: 0 blue, 1 gold, 2 green, 3 purple, 4 red.
    fun themeIndex(ctx: Context): Int = sp(ctx).getInt(KEY_THEME, 0)

    fun setThemeIndex(ctx: Context, i: Int) {
        sp(ctx).edit().putInt(KEY_THEME, i).apply()
    }

    fun themeStyle(ctx: Context): Int = when (themeIndex(ctx)) {
        1 -> com.zarwa.launcher.R.style.Theme_ZarwaLauncher_Gold
        2 -> com.zarwa.launcher.R.style.Theme_ZarwaLauncher_Green
        3 -> com.zarwa.launcher.R.style.Theme_ZarwaLauncher_Purple
        4 -> com.zarwa.launcher.R.style.Theme_ZarwaLauncher_Red
        5 -> com.zarwa.launcher.R.style.Theme_ZarwaLauncher_Turquoise
        6 -> com.zarwa.launcher.R.style.Theme_ZarwaLauncher_Orange
        7 -> com.zarwa.launcher.R.style.Theme_ZarwaLauncher_Pink
        8 -> com.zarwa.launcher.R.style.Theme_ZarwaLauncher_Silver
        else -> com.zarwa.launcher.R.style.Theme_ZarwaLauncher
    }
}

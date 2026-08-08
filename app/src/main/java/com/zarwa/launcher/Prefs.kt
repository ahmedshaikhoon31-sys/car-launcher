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
}

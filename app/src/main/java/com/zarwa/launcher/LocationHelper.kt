package com.zarwa.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Best-effort GPS location -> weather coordinates + city name.
 * Every call is fully guarded; it never throws and never blocks the UI thread
 * on network work (reverse-geocoding runs on a background thread).
 */
object LocationHelper {

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Reads the last known GPS/network fix, stores the coordinates, then reverse-geocodes
     * the city name in the background. [onUpdated] is called on the main thread whenever
     * something changed (coordinates first, then again if the city name resolves).
     */
    fun refresh(ctx: Context, onUpdated: (() -> Unit)? = null) {
        try {
            if (!hasPermission(ctx)) return
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val loc = try {
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            } catch (e: SecurityException) {
                null
            } ?: return

            Prefs.setLatLon(ctx, loc.latitude, loc.longitude)
            onUpdated?.invoke()

            // Reverse-geocode the city name off the UI thread (needs network).
            Thread {
                val city = try {
                    val locale = ctx.resources.configuration.locales[0]
                    @Suppress("DEPRECATION")
                    val addrs = Geocoder(ctx, locale).getFromLocation(loc.latitude, loc.longitude, 1)
                    addrs?.firstOrNull()?.let {
                        it.locality ?: it.subAdminArea ?: it.adminArea
                    }
                } catch (e: Throwable) {
                    null
                }
                if (!city.isNullOrBlank()) {
                    Prefs.setCity(ctx, city)
                    Handler(Looper.getMainLooper()).post { onUpdated?.invoke() }
                }
            }.start()
        } catch (e: Throwable) {
            // Location is a best-effort enhancement — never let it crash the launcher.
        }
    }
}

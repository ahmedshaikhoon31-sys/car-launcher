package com.zarwa.launcher

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.zarwa.launcher.databinding.ActivitySpeedBinding

/** Tesla-style GPS speedometer — works without any CAN connection. */
class SpeedActivity : AppCompatActivity(), LocationListener {

    private lateinit var b: ActivitySpeedBinding
    private var lm: LocationManager? = null
    private var listening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.applyTheme(Prefs.isDark(this))
        super.onCreate(savedInstanceState)
        setTheme(Prefs.themeStyle(this))
        b = ActivitySpeedBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.aurora.style = Prefs.bgStyle(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        b.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        startUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopUpdates()
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun startUpdates() {
        if (listening) return
        if (!hasPermission()) {
            b.speedHint.text = getString(R.string.gps_waiting)
            return
        }
        try {
            lm = getSystemService(LOCATION_SERVICE) as LocationManager
            lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
            listening = true
        } catch (e: Throwable) {
            b.speedHint.text = getString(R.string.gps_waiting)
        }
    }

    private fun stopUpdates() {
        try {
            lm?.removeUpdates(this)
        } catch (e: Throwable) {
        }
        listening = false
    }

    override fun onLocationChanged(location: Location) {
        val kmh = if (location.hasSpeed()) (location.speed * 3.6f) else 0f
        b.speedValue.text = kmh.toInt().coerceAtLeast(0).toString()
        b.speedHint.text = "GPS"
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}

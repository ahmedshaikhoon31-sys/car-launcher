package com.zarwa.launcher

import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zarwa.launcher.databinding.ActivityControlBinding

/** Tesla/Mercedes-style quick control panel: brightness, volume, toggles. */
class ControlActivity : AppCompatActivity() {

    private lateinit var b: ActivityControlBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.applyTheme(Prefs.isDark(this))
        super.onCreate(savedInstanceState)
        setTheme(Prefs.themeStyle(this))
        b = ActivityControlBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.aurora.style = Prefs.bgStyle(this)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        b.btnBack.setOnClickListener { finish() }

        setupVolume()
        setupBrightness()

        b.swAutoNight.isChecked = Prefs.autoNight(this)
        b.swAutoNight.setOnCheckedChangeListener { _, on -> Prefs.setAutoNight(this, on) }

        b.tileSpeed.setOnClickListener { startActivity(Intent(this, SpeedActivity::class.java)) }
        b.tileWifi.setOnClickListener { AppLauncher.openWifiSettings(this) }
        b.tileBt.setOnClickListener { AppLauncher.openBluetoothSettings(this) }
        b.tileSettings.setOnClickListener { AppLauncher.openSettings(this) }
    }

    private fun setupVolume() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        b.seekVol.max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        b.seekVol.progress = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        b.seekVol.setOnSeekBarChangeListener(simple { p, fromUser ->
            if (fromUser) am.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0)
        })
    }

    private fun setupBrightness() {
        val canWrite = Settings.System.canWrite(this)
        if (canWrite) {
            b.seekBright.progress = try {
                Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            } catch (e: Exception) { 128 }
        }
        b.seekBright.setOnSeekBarChangeListener(simple { p, fromUser ->
            if (!fromUser) return@simple
            if (!Settings.System.canWrite(this)) {
                requestWriteSettings()
            } else {
                try {
                    Settings.System.putInt(
                        contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                    Settings.System.putInt(
                        contentResolver, Settings.System.SCREEN_BRIGHTNESS, p.coerceIn(5, 255)
                    )
                } catch (e: Exception) {
                }
            }
        })
    }

    private fun requestWriteSettings() {
        Toast.makeText(this, R.string.need_write_settings, Toast.LENGTH_LONG).show()
        try {
            startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
        }
    }

    private inline fun simple(crossinline onChange: (Int, Boolean) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) = onChange(p, fromUser)
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        }
}

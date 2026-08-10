package com.zarwa.launcher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.content.Intent
import androidx.core.content.ContextCompat
import com.zarwa.launcher.can.CanBus
import com.zarwa.launcher.can.CanReceiver
import com.zarwa.launcher.databinding.FragmentDashboardBinding
import com.zarwa.launcher.media.MediaHub
import com.zarwa.launcher.weather.WeatherActivity
import com.zarwa.launcher.weather.WeatherRepo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _b: FragmentDashboardBinding? = null
    private val b get() = _b!!

    private val handler = Handler(Looper.getMainLooper())
    private val arLocale = Locale("ar")
    private val timeFmt = SimpleDateFormat("h:mm", Locale.US)
    private val ampmFmt = SimpleDateFormat("a", Locale.US)
    private val dateFmt = SimpleDateFormat("EEEE، d MMMM", arLocale)
    private var lastWeatherFetch = 0L
    private var canReceiver: CanReceiver? = null

    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            updateMedia()
            maybeRefreshWeather()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return try {
            _b = FragmentDashboardBinding.inflate(inflater, container, false)
            b.root
        } catch (e: Throwable) {
            errorView(inflater.context, "الداشبورد", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (_b == null) return
        val ctx = requireContext()

        // Media controls
        b.btnPrev.setOnClickListener { MediaHub.previous(ctx) }
        b.btnNext.setOnClickListener { MediaHub.next(ctx) }
        b.btnPlay.setOnClickListener {
            if (!MediaHub.hasNotificationAccess(ctx)) MediaHub.openNotificationAccessSettings(ctx)
            else MediaHub.playPause(ctx)
        }
        b.mediaHint.setOnClickListener { MediaHub.openNotificationAccessSettings(ctx) }

        // Navigation card
        b.navCard.setOnClickListener { AppLauncher.openMaps(ctx) }

        // Weather chip -> hourly forecast screen
        b.weatherRow.setOnClickListener {
            startActivity(Intent(ctx, WeatherActivity::class.java))
        }

        // Quick-action buttons (useful on any unit — no CAN needed)
        b.btnClimate.statusIcon.setImageResource(R.drawable.ic_wifi)
        b.btnClimate.statusTitle.text = getString(R.string.wifi)
        b.btnClimate.statusSub.text = getString(R.string.quick_conn)
        b.btnClimate.root.setOnClickListener { AppLauncher.openWifiSettings(ctx) }

        b.btnFuel.statusIcon.setImageResource(R.drawable.ic_bluetooth)
        b.btnFuel.statusTitle.text = getString(R.string.bluetooth)
        b.btnFuel.statusSub.text = getString(R.string.quick_conn)
        b.btnFuel.root.setOnClickListener { AppLauncher.openBluetoothSettings(ctx) }

        b.btnCar.statusIcon.setImageResource(R.drawable.ic_settings)
        b.btnCar.statusTitle.text = getString(R.string.settings)
        b.btnCar.statusSub.text = getString(R.string.quick_system)
        b.btnCar.root.setOnClickListener { AppLauncher.openSettings(ctx) }

        // Dock
        b.dockPhone.setOnClickListener { AppLauncher.openPhone(ctx) }
        b.dockMaps.setOnClickListener { AppLauncher.openMaps(ctx) }
        b.dockVideo.setOnClickListener { AppLauncher.openVideo(ctx) }
        b.dockRadio.setOnClickListener { AppLauncher.openRadio(ctx) }
        b.dockCamera.setOnClickListener { AppLauncher.openCamera(ctx) }
        b.dockSplit.setOnClickListener { AppLauncher.openDrawerForSplit(ctx) }
        b.dockSettings.setOnClickListener { AppLauncher.openSettings(ctx) }
        b.dockApps.setOnClickListener { AppLauncher.openDrawer(ctx) }

        // Premium entrance: staggered fade + rise
        val entrance = listOf(b.txtClock, b.weatherRow, b.mediaCard, b.navCard, b.statusRow, b.dock)
        entrance.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 28f
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(80L + i * 70L)
                .setDuration(420L)
                .start()
        }
    }

    private fun updateClock() {
        val now = Date()
        b.txtClock.text = timeFmt.format(now)
        b.txtAmPm.text = ampmFmt.format(now)
        b.txtDate.text = dateFmt.format(now)
    }

    private fun updateMedia() {
        val ctx = context ?: return
        if (!MediaHub.hasNotificationAccess(ctx)) {
            b.txtTrack.text = getString(R.string.now_playing)
            b.txtArtist.text = getString(R.string.tap_to_play)
            b.mediaHint.visibility = View.VISIBLE
            b.mediaProgress.progress = 0
            b.albumArt.setImageDrawable(null)
            b.btnPlay.setImageResource(R.drawable.ic_play)
            return
        }
        b.mediaHint.visibility = View.GONE
        val np = MediaHub.nowPlaying(ctx)
        if (np == null) {
            b.txtTrack.text = getString(R.string.now_playing)
            b.txtArtist.text = getString(R.string.tap_to_play)
            b.mediaProgress.progress = 0
            b.albumArt.setImageDrawable(null)
            b.btnPlay.setImageResource(R.drawable.ic_play)
            return
        }
        b.txtTrack.text = np.title
        b.txtArtist.text = np.artist
        if (np.art != null) b.albumArt.setImageBitmap(np.art) else b.albumArt.setImageDrawable(null)
        b.mediaProgress.progress = if (np.progress >= 0) (np.progress * 100).toInt() else 0
        b.btnPlay.setImageResource(if (np.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    /** Fill climate/fuel/car widgets from CAN data if any arrived (see CanBus). */
    private fun updateCan() {
        val l = CanBus.climateLeft
        val r = CanBus.climateRight
        if (l != null || r != null) {
            b.btnClimate.statusTitle.text = "${l ?: "--"}° / ${r ?: "--"}°"
            b.btnClimate.statusSub.text = getString(R.string.climate)
        }
        CanBus.rangeKm?.let {
            b.btnFuel.statusTitle.text = "$it كم"
            b.btnFuel.statusSub.text = getString(R.string.fuel_range)
        }
        CanBus.doorsLocked?.let {
            b.btnCar.statusTitle.text = if (it) "مغلقة" else "مفتوحة"
            b.btnCar.statusSub.text = getString(R.string.car_status)
        }
    }

    private fun maybeRefreshWeather() {
        val now = System.currentTimeMillis()
        if (now - lastWeatherFetch < 15 * 60 * 1000L) return
        lastWeatherFetch = now
        val ctx = context ?: return
        WeatherRepo.fetch(Prefs.lat(ctx), Prefs.lon(ctx)) { w ->
            val bind = _b ?: return@fetch
            if (w == null) return@fetch
            bind.weatherTemp.text = "${w.tempC}°"
            bind.weatherDesc.text = "${Prefs.city(ctx)} · ${w.desc}"
            bind.weatherIcon.setImageResource(w.iconRes)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_b == null) return
        handler.post(tick)
        // CAN receiver disabled (no CAN box on this unit, and it's a reboot suspect).
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

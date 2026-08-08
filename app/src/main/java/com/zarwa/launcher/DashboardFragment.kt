package com.zarwa.launcher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.zarwa.launcher.databinding.FragmentDashboardBinding
import com.zarwa.launcher.media.MediaHub
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
        _b = FragmentDashboardBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        // Status widgets (decorative until CAN is wired — honest labels + tap opens related app)
        b.btnClimate.statusIcon.setImageResource(R.drawable.ic_climate)
        b.btnClimate.statusTitle.text = getString(R.string.climate)
        b.btnClimate.statusSub.text = getString(R.string.needs_can)
        b.btnClimate.root.setOnClickListener { AppLauncher.toast(ctx, getString(R.string.needs_can)) }

        b.btnFuel.statusIcon.setImageResource(R.drawable.ic_fuel)
        b.btnFuel.statusTitle.text = getString(R.string.fuel_range)
        b.btnFuel.statusSub.text = getString(R.string.needs_can)
        b.btnFuel.root.setOnClickListener { AppLauncher.toast(ctx, getString(R.string.needs_can)) }

        b.btnCar.statusIcon.setImageResource(R.drawable.ic_car)
        b.btnCar.statusTitle.text = getString(R.string.car_status)
        b.btnCar.statusSub.text = getString(R.string.needs_can)
        b.btnCar.root.setOnClickListener { AppLauncher.toast(ctx, getString(R.string.needs_can)) }

        // Dock
        b.dockPhone.setOnClickListener { AppLauncher.openPhone(ctx) }
        b.dockMaps.setOnClickListener { AppLauncher.openMaps(ctx) }
        b.dockVideo.setOnClickListener { AppLauncher.openVideo(ctx) }
        b.dockRadio.setOnClickListener { AppLauncher.openRadio(ctx) }
        b.dockCamera.setOnClickListener { AppLauncher.openCamera(ctx) }
        b.dockSettings.setOnClickListener { AppLauncher.openSettings(ctx) }
        b.dockApps.setOnClickListener { AppLauncher.openDrawer(ctx) }
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
        handler.post(tick)
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

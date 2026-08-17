package com.zarwa.launcher

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.animation.AccelerateDecelerateInterpolator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.zarwa.launcher.weather.HourWeather
import androidx.fragment.app.Fragment
import android.content.Intent
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
    private val hourFmt = SimpleDateFormat("H", Locale.US)
    private val dateFmt = SimpleDateFormat("EEEE، d MMMM", arLocale)
    private var lastWeatherFetch = 0L
    private val eqAnimators = mutableListOf<ObjectAnimator>()
    private var eqRunning = false
    private val microAnimators = mutableListOf<ObjectAnimator>()
    private var lastTrackForColor: String? = null

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

        setupEqualizer()
        startMicroMotion()

        // Premium entrance: staggered fade + rise
        val entrance = listOf(b.txtGreeting, b.txtClock, b.weatherRow, b.mediaCard, b.navCard, b.statusRow, b.dock)
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
        val hour = hourFmt.format(now).toIntOrNull() ?: 12
        b.txtGreeting.setText(
            when {
                hour < 12 -> R.string.greet_morning
                hour < 18 -> R.string.greet_afternoon
                else -> R.string.greet_evening
            }
        )
    }

    private fun updateMedia() {
        val ctx = context ?: return
        if (!MediaHub.hasNotificationAccess(ctx)) {
            b.txtTrack.text = getString(R.string.now_playing)
            b.txtArtist.text = getString(R.string.tap_to_play)
            b.mediaProgress.progress = 0
            b.albumArt.setImageDrawable(null)
            b.btnPlay.setImageResource(R.drawable.ic_play)
            setEqualizerPlaying(false)
            return
        }
        val np = MediaHub.nowPlaying(ctx)
        if (np == null) {
            b.txtTrack.text = getString(R.string.now_playing)
            b.txtArtist.text = getString(R.string.tap_to_play)
            b.mediaProgress.progress = 0
            b.albumArt.setImageDrawable(null)
            b.btnPlay.setImageResource(R.drawable.ic_play)
            setEqualizerPlaying(false)
            return
        }
        b.txtTrack.text = np.title
        b.txtArtist.text = np.artist
        b.mediaProgress.progress = if (np.progress >= 0) (np.progress * 100).toInt() else 0
        b.btnPlay.setImageResource(if (np.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        setEqualizerPlaying(np.isPlaying)
        val art = np.art
        if (art != null) {
            b.albumArt.setImageBitmap(art)
            if (np.title != lastTrackForColor) {   // recolour media to the song (Lucid-style)
                lastTrackForColor = np.title
                androidx.palette.graphics.Palette.from(art).generate { p ->
                    if (_b == null || p == null) return@generate
                    applyMediaColor(p.getVibrantColor(p.getDominantColor(accentColor())))
                }
            }
        } else {
            b.albumArt.setImageDrawable(null)
            if (lastTrackForColor != null) { lastTrackForColor = null; applyMediaColor(accentColor()) }
        }
    }

    private fun accentColor(): Int {
        val ctx = context ?: return 0xFF4FC3F7.toInt()
        val tv = android.util.TypedValue()
        ctx.theme.resolveAttribute(R.attr.accent, tv, true)
        return if (tv.data != 0) tv.data else 0xFF4FC3F7.toInt()
    }

    private fun applyMediaColor(color: Int) {
        val bind = _b ?: return
        val csl = android.content.res.ColorStateList.valueOf(color)
        bind.mediaProgress.progressTintList = csl
        bind.btnPlay.backgroundTintList = csl
        for (i in 0 until bind.eqBars.childCount) bind.eqBars.getChildAt(i).setBackgroundColor(color)
    }

    private fun setupEqualizer() {
        val ctx = context ?: return
        b.eqBars.removeAllViews()
        eqAnimators.forEach { it.cancel() }
        eqAnimators.clear()
        val d = resources.displayMetrics.density
        val barW = (4 * d).toInt()
        val gap = (2 * d).toInt()
        repeat(4) { i ->
            val bar = View(ctx)
            val lp = LinearLayout.LayoutParams(barW, LinearLayout.LayoutParams.MATCH_PARENT)
            lp.marginStart = gap; lp.marginEnd = gap
            bar.layoutParams = lp
            bar.setBackgroundColor(ContextCompat.getColor(ctx, R.color.accent))
            b.eqBars.addView(bar)
            bar.post { bar.pivotY = bar.height.toFloat() }
            eqAnimators.add(
                ObjectAnimator.ofFloat(bar, "scaleY", 0.25f, 1f).apply {
                    duration = 320L + i * 130L
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                }
            )
        }
    }

    private fun setEqualizerPlaying(playing: Boolean) {
        if (playing == eqRunning) return
        eqRunning = playing
        if (playing) {
            b.eqBars.visibility = View.VISIBLE
            eqAnimators.forEach { it.start() }
        } else {
            b.eqBars.visibility = View.GONE
            eqAnimators.forEach { it.cancel() }
        }
    }

    /** Subtle continuous life: weather icon floats, play button gently breathes. */
    private fun startMicroMotion() {
        microAnimators.forEach { it.cancel() }
        microAnimators.clear()
        val d = resources.displayMetrics.density
        microAnimators.add(
            ObjectAnimator.ofFloat(b.weatherIcon, "translationY", 0f, -5f * d).apply {
                duration = 2200; repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator(); start()
            }
        )
        microAnimators.add(
            ObjectAnimator.ofPropertyValuesHolder(
                b.btnPlay,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.06f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.06f)
            ).apply {
                duration = 1500; repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator(); start()
            }
        )
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
        WeatherRepo.fetchHourly(Prefs.lat(ctx), Prefs.lon(ctx), 4) { hours ->
            if (_b != null) buildHourlyStrip(hours)
        }
    }

    /** Inline next-hours forecast that fills the empty left space. */
    private fun buildHourlyStrip(hours: List<HourWeather>) {
        val bind = _b ?: return
        val ctx = context ?: return
        bind.hourlyStrip.removeAllViews()
        val d = resources.displayMetrics.density
        hours.take(4).forEach { h ->
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.search_bg)
                setPadding((6 * d).toInt(), (10 * d).toInt(), (6 * d).toInt(), (10 * d).toInt())
            }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = (4 * d).toInt(); lp.marginEnd = (4 * d).toInt()
            col.layoutParams = lp
            col.addView(TextView(ctx).apply {
                text = h.label
                setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                textSize = 12f
            })
            col.addView(ImageView(ctx).apply {
                setImageResource(h.iconRes)
                layoutParams = LinearLayout.LayoutParams((26 * d).toInt(), (26 * d).toInt()).also {
                    it.topMargin = (6 * d).toInt(); it.bottomMargin = (6 * d).toInt()
                }
            })
            col.addView(TextView(ctx).apply {
                text = "${h.tempC}°"
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                textSize = 15f
            })
            bind.hourlyStrip.addView(col)
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
        microAnimators.forEach { it.cancel() }
        microAnimators.clear()
        eqAnimators.forEach { it.cancel() }
        eqAnimators.clear()
        _b = null
    }
}

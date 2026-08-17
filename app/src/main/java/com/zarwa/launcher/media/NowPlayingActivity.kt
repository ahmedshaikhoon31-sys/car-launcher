package com.zarwa.launcher.media

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import com.zarwa.launcher.Prefs
import com.zarwa.launcher.R
import com.zarwa.launcher.databinding.ActivityNowPlayingBinding

/** Full-screen premium player: big art, colour glow, equalizer, seek + volume. */
class NowPlayingActivity : AppCompatActivity() {

    private lateinit var b: ActivityNowPlayingBinding
    private val handler = Handler(Looper.getMainLooper())
    private val eqAnimators = mutableListOf<ObjectAnimator>()
    private var eqRunning = false
    private var seeking = false
    private var lastArtKey: String? = null
    private var accent = 0xFF4FC3F7.toInt()

    private val tick = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.applyTheme(Prefs.isDark(this))
        super.onCreate(savedInstanceState)
        setTheme(Prefs.themeStyle(this))
        b = ActivityNowPlayingBinding.inflate(layoutInflater)
        setContentView(b.root)
        com.zarwa.launcher.FontUtil.apply(this, b.root)
        b.aurora.style = Prefs.bgStyle(this)

        accent = resolveAccent()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        b.btnBack.setOnClickListener { finish() }
        b.npPrev.setOnClickListener { MediaHub.previous(this); handler.postDelayed({ refresh() }, 250) }
        b.npNext.setOnClickListener { MediaHub.next(this); handler.postDelayed({ refresh() }, 250) }
        b.npPlay.setOnClickListener {
            if (!MediaHub.hasNotificationAccess(this)) MediaHub.openNotificationAccessSettings(this)
            else { MediaHub.playPause(this); handler.postDelayed({ refresh() }, 200) }
        }

        buildEqualizer()
        setupVolume()

        b.btnEq.setOnClickListener { startActivity(android.content.Intent(this, EqualizerActivity::class.java)) }
        b.npVolIcon.setOnClickListener { toggleMute() }

        b.npSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(sb: SeekBar) { seeking = true }
            override fun onStopTrackingTouch(sb: SeekBar) {
                val np = MediaHub.nowPlaying(this@NowPlayingActivity)
                val dur = np?.durationMs ?: -1L
                if (dur > 0) MediaHub.seekTo(this@NowPlayingActivity, dur * sb.progress / 1000)
                seeking = false
            }
        })
    }

    private val am by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private var volBeforeMute = 0

    private fun setupVolume() {
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        b.npVol.max = max
        b.npVol.progress = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        b.npVol.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) am.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    /** Tap the speaker icon to mute / restore volume. */
    private fun toggleMute() {
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (cur > 0) {
            volBeforeMute = cur
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            b.npVol.progress = 0
        } else {
            val restore = if (volBeforeMute > 0) volBeforeMute
                else am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2
            am.setStreamVolume(AudioManager.STREAM_MUSIC, restore, 0)
            b.npVol.progress = restore
        }
    }

    private fun refresh() {
        val np = if (MediaHub.hasNotificationAccess(this)) MediaHub.nowPlaying(this) else null
        if (np == null) {
            b.npTitle.text = getString(R.string.now_playing)
            b.npArtist.text = getString(R.string.tap_to_play)
            b.npPlay.setImageResource(R.drawable.ic_play)
            b.npArt.setImageDrawable(null)
            setEq(false)
            b.npSeek.progress = 0
            b.npCur.text = fmt(0); b.npDur.text = fmt(0)
            return
        }
        b.npTitle.text = np.title
        b.npArtist.text = np.artist
        b.npPlay.setImageResource(if (np.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        setEq(np.isPlaying)

        if (!seeking) {
            if (np.durationMs > 0) {
                b.npSeek.progress = ((np.positionMs.coerceAtLeast(0) * 1000) / np.durationMs).toInt()
                b.npCur.text = fmt(np.positionMs)
                b.npDur.text = fmt(np.durationMs)
            } else {
                b.npSeek.progress = 0
                b.npCur.text = fmt(0); b.npDur.text = fmt(0)
            }
        }

        val art = np.art
        if (art != null) {
            b.npArt.setImageBitmap(art)
            val key = "${np.title}|${np.artist}"
            if (key != lastArtKey) {
                lastArtKey = key
                Palette.from(art).generate { p ->
                    if (p == null) return@generate
                    applyColor(p.getVibrantColor(p.getDominantColor(accent)))
                }
            }
        } else {
            b.npArt.setImageDrawable(null)
            if (lastArtKey != null) { lastArtKey = null; applyColor(accent) }
        }
    }

    private fun applyColor(color: Int) {
        val csl = ColorStateList.valueOf(color)
        b.npPlay.backgroundTintList = csl
        b.npSeek.progressTintList = csl
        b.npSeek.thumbTintList = csl
        b.npVol.progressTintList = csl
        b.npVol.thumbTintList = csl
        for (i in 0 until b.npEq.childCount) b.npEq.getChildAt(i).setBackgroundColor(color)
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            b.npArt.outlineAmbientShadowColor = color
            b.npArt.outlineSpotShadowColor = color
        }
    }

    private fun buildEqualizer() {
        b.npEq.removeAllViews()
        eqAnimators.forEach { it.cancel() }
        eqAnimators.clear()
        val d = resources.displayMetrics.density
        val barW = (5 * d).toInt()
        val gap = (3 * d).toInt()
        repeat(6) { i ->
            val bar = View(this)
            val lp = LinearLayout.LayoutParams(barW, LinearLayout.LayoutParams.MATCH_PARENT)
            lp.marginStart = gap; lp.marginEnd = gap
            bar.layoutParams = lp
            bar.setBackgroundColor(accent)
            b.npEq.addView(bar)
            bar.post { bar.pivotY = bar.height.toFloat() }
            eqAnimators.add(
                ObjectAnimator.ofFloat(bar, "scaleY", 0.22f, 1f).apply {
                    duration = 300L + i * 110L
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                }
            )
        }
    }

    private fun setEq(playing: Boolean) {
        if (playing == eqRunning) return
        eqRunning = playing
        if (playing) {
            b.npEq.visibility = View.VISIBLE
            eqAnimators.forEach { it.start() }
        } else {
            b.npEq.visibility = View.INVISIBLE
            eqAnimators.forEach { it.cancel() }
        }
    }

    private fun resolveAccent(): Int {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(R.attr.accent, tv, true)
        return if (tv.data != 0) tv.data else 0xFF4FC3F7.toInt()
    }

    private fun fmt(ms: Long): String {
        if (ms <= 0) return "0:00"
        val total = ms / 1000
        return "%d:%02d".format(total / 60, total % 60)
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    override fun onDestroy() {
        super.onDestroy()
        eqAnimators.forEach { it.cancel() }
        eqAnimators.clear()
    }
}

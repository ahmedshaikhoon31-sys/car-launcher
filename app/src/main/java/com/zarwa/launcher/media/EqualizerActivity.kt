package com.zarwa.launcher.media

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.zarwa.launcher.Prefs
import com.zarwa.launcher.R
import com.zarwa.launcher.databinding.ActivityEqualizerBinding

/** Full audio equalizer (Mercedes-style): per-band sliders, presets, bass, surround. */
class EqualizerActivity : AppCompatActivity() {

    private lateinit var b: ActivityEqualizerBinding
    private val bandSeeks = mutableListOf<SeekBar>()
    private var accent = 0xFF4FC3F7.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.applyTheme(Prefs.isDark(this))
        super.onCreate(savedInstanceState)
        setTheme(Prefs.themeStyle(this))
        b = ActivityEqualizerBinding.inflate(layoutInflater)
        setContentView(b.root)
        com.zarwa.launcher.FontUtil.apply(this, b.root)
        b.aurora.style = Prefs.bgStyle(this)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        accent = resolveAccent()
        b.btnBack.setOnClickListener { finish() }

        AudioFx.ensure()
        val eq = AudioFx.eq
        if (eq == null) {
            showUnsupported()
            return
        }
        buildBands(eq)
        buildPresets(eq)
        setupBass()
        setupSurround()
    }

    private fun showUnsupported() {
        b.eqNote.visibility = View.VISIBLE
        b.eqBands.visibility = View.GONE
        b.eqPresetLabel.visibility = View.GONE
        b.eqPresetScroll.visibility = View.GONE
        b.bassLabel.visibility = View.GONE
        b.seekBass.visibility = View.GONE
        b.surroundLabel.visibility = View.GONE
        b.seekSurround.visibility = View.GONE
    }

    private fun buildBands(eq: android.media.audiofx.Equalizer) {
        b.eqBands.removeAllViews()
        bandSeeks.clear()
        val range = eq.bandLevelRange // [min, max] in millibels
        val min = range[0].toInt()
        val max = range[1].toInt()
        val span = (max - min).coerceAtLeast(1)
        val bands = eq.numberOfBands.toInt()
        for (i in 0 until bands) {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            val lp = LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginStart = dp(4); lp.marginEnd = dp(4)
            col.layoutParams = lp

            // vertical slider inside a fixed frame (rotated horizontal SeekBar)
            val frame = FrameLayout(this)
            frame.layoutParams = LinearLayout.LayoutParams(dp(56), dp(200))
            val seek = SeekBar(this).apply {
                this.max = span
                progress = (eq.getBandLevel(i.toShort()) - min).coerceIn(0, span)
                rotation = 270f
                progressTintList = android.content.res.ColorStateList.valueOf(accent)
                thumbTintList = android.content.res.ColorStateList.valueOf(accent)
            }
            seek.layoutParams = FrameLayout.LayoutParams(dp(200), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    if (fromUser) try { eq.setBandLevel(i.toShort(), (min + p).toShort()) } catch (e: Exception) {}
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
            frame.addView(seek)
            bandSeeks.add(seek)

            val freq = TextView(this).apply {
                text = freqLabel(eq.getCenterFreq(i.toShort()))
                setTextColor(ContextCompat.getColor(this@EqualizerActivity, R.color.text_secondary))
                textSize = 12f
                gravity = Gravity.CENTER
            }
            val freqLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            freqLp.topMargin = dp(6)
            freq.layoutParams = freqLp

            col.addView(frame)
            col.addView(freq)
            b.eqBands.addView(col)
        }
    }

    private fun buildPresets(eq: android.media.audiofx.Equalizer) {
        b.eqPresets.removeAllViews()
        val count = eq.numberOfPresets.toInt()
        if (count == 0) { b.eqPresetLabel.visibility = View.GONE; return }
        for (p in 0 until count) {
            val name = try { eq.getPresetName(p.toShort()) } catch (e: Exception) { "#$p" }
            val chip = Button(this).apply {
                text = name
                isAllCaps = false
                textSize = 13f
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.search_bg)
                setPadding(dp(18), dp(10), dp(18), dp(10))
                setOnClickListener {
                    try {
                        eq.usePreset(p.toShort())
                        refreshBands(eq)
                    } catch (e: Exception) {}
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp(10)
            chip.layoutParams = lp
            b.eqPresets.addView(chip)
        }
    }

    private fun refreshBands(eq: android.media.audiofx.Equalizer) {
        val min = eq.bandLevelRange[0].toInt()
        bandSeeks.forEachIndexed { i, sb ->
            try { sb.progress = (eq.getBandLevel(i.toShort()) - min).coerceAtLeast(0) } catch (e: Exception) {}
        }
    }

    private fun setupBass() {
        val bass = AudioFx.bass
        if (bass == null || !bass.strengthSupported) { b.bassLabel.visibility = View.GONE; b.seekBass.visibility = View.GONE; return }
        b.seekBass.progress = try { bass.roundedStrength.toInt() } catch (e: Exception) { 0 }
        b.seekBass.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) try { bass.setStrength(p.toShort()) } catch (e: Exception) {}
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun setupSurround() {
        val virt = AudioFx.virt
        if (virt == null || !virt.strengthSupported) { b.surroundLabel.visibility = View.GONE; b.seekSurround.visibility = View.GONE; return }
        b.seekSurround.progress = try { virt.roundedStrength.toInt() } catch (e: Exception) { 0 }
        b.seekSurround.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) try { virt.setStrength(p.toShort()) } catch (e: Exception) {}
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun freqLabel(milliHz: Int): String {
        val hz = milliHz / 1000
        return if (hz >= 1000) "${hz / 1000}k" else "$hz"
    }

    private fun resolveAccent(): Int {
        val tv = android.util.TypedValue()
        theme.resolveAttribute(R.attr.accent, tv, true)
        return if (tv.data != 0) tv.data else 0xFF4FC3F7.toInt()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

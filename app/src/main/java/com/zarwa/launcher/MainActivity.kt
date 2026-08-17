package com.zarwa.launcher

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.zarwa.launcher.databinding.ActivityMainBinding
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private companion object {
        const val REQ_LOCATION = 42
        // Show the welcome screen once per process (i.e. once per real boot/start),
        // not on every configuration-driven recreate.
        var welcomeShown = false
    }

    private lateinit var b: ActivityMainBinding

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            Prefs.setBgUri(this, uri.toString())
            applyBackground()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.applyTheme(Prefs.isDark(this))
        super.onCreate(savedInstanceState)
        setTheme(Prefs.themeStyle(this))
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setImmersive()
        applyBackground()
        b.aurora.style = Prefs.bgStyle(this)

        b.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int) = when (position) {
                0 -> DashboardFragment()
                1 -> CustomFragment()
                else -> AppsFragment()
            }
        }
        // Default offscreen limit: only the visible page loads at startup → faster
        // first frame as HOME (important on low-RAM units).

        buildDots(3)
        b.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateDots(position)
            override fun onPageScrolled(position: Int, offset: Float, offsetPx: Int) {
                // Subtle parallax: the living background drifts slower than the pages.
                b.aurora.translationX = -(position + offset) * 55f
            }
        })
        updateDots(0)

        // Smooth premium page transition (subtle fade + zoom).
        b.pager.setPageTransformer { page, position ->
            val abs = Math.abs(position)
            page.alpha = (1f - 0.35f * abs).coerceIn(0.4f, 1f)
            val scale = (1f - 0.05f * abs).coerceIn(0.9f, 1f)
            page.scaleX = scale
            page.scaleY = scale
        }

        b.btnTheme.setOnClickListener {
            Prefs.setDark(this, !Prefs.isDark(this))
            recreate() // uiMode is in configChanges, so force re-inflation with the new theme
        }
        b.btnTheme.setOnLongClickListener { showThemePicker(); true }

        b.btnWall.setOnClickListener { showWallpaperMenu() }

        ensureLocationPermission()
        maybeShowWelcome(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        applyNightDim()
    }

    /** Softly dims the screen during night hours when auto-night is on. */
    private fun applyNightDim() {
        val target = if (Prefs.autoNight(this) && isNightHour()) 0.32f else 0f
        b.nightDim.animate().alpha(target).setDuration(600L).start()
    }

    private fun isNightHour(): Boolean {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return h >= 19 || h < 6
    }

    /** Premium branded intro: brand name + personal greeting, then fades away. */
    private fun maybeShowWelcome(savedInstanceState: Bundle?) {
        if (welcomeShown || savedInstanceState != null) {
            b.welcome.visibility = View.GONE
            return
        }
        welcomeShown = true
        try {
            val name = Prefs.userName(this)
            if (name.isEmpty()) {
                b.welcomeHello.visibility = View.GONE
                b.welcomeGreeting.text = timeGreeting()
            } else {
                b.welcomeHello.visibility = View.VISIBLE
                b.welcomeHello.text = timeGreeting()
                b.welcomeGreeting.text = name
            }
            b.welcomeClock.text = java.text.SimpleDateFormat("h:mm", java.util.Locale.US).format(java.util.Date())
            b.welcomeReady.text = randomTagline()
            applyWelcomeBackground()

            val w = b.welcome
            w.visibility = View.VISIBLE
            w.alpha = 1f
            // Gentle rise-in of the greeting block.
            val block = b.welcomeBlock
            block.alpha = 0f
            block.translationY = 24f
            // Start the hold+fade only AFTER the welcome is actually drawn, so on a
            // slow cold boot it stays on screen for its full duration (never dismissed
            // before the first frame renders).
            w.post {
                block.animate().alpha(1f).translationY(0f)
                    .setStartDelay(120L).setDuration(600L).start()
                shimmer(b.welcomeGreeting)
                w.postDelayed({
                    w.animate().alpha(0f).setDuration(500L)
                        .withEndAction { w.visibility = View.GONE }.start()
                }, 2200L)
            }
        } catch (e: Throwable) {
            b.welcome.visibility = View.GONE
        }
    }

    /** Picks a warm, varied tagline each start. */
    private fun randomTagline(): String {
        val arr = resources.getStringArray(R.array.welcome_taglines)
        if (arr.isEmpty()) return getString(R.string.welcome_ready)
        val i = (System.currentTimeMillis() % arr.size).toInt()
        return arr[i]
    }

    /** Uses the user's chosen home background behind the welcome (with a dark scrim). */
    private fun applyWelcomeBackground() {
        val uriStr = Prefs.bgUri(this)
        if (uriStr == null) {
            b.welcomeBgImage.visibility = View.GONE
            b.welcomeScrim.visibility = View.GONE
            return
        }
        try {
            contentResolver.openInputStream(Uri.parse(uriStr)).use { input ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                val bmp = BitmapFactory.decodeStream(input, null, opts)
                if (bmp != null) {
                    b.welcomeBgImage.setImageBitmap(bmp)
                    b.welcomeBgImage.visibility = View.VISIBLE
                    b.welcomeScrim.visibility = View.VISIBLE
                } else {
                    b.welcomeBgImage.visibility = View.GONE
                    b.welcomeScrim.visibility = View.GONE
                }
            }
        } catch (e: Throwable) {
            b.welcomeBgImage.visibility = View.GONE
            b.welcomeScrim.visibility = View.GONE
        }
    }

    /** A soft accent-tinted light sweep across the given text once. */
    private fun shimmer(tv: android.widget.TextView) {
        tv.post {
            val w = tv.width.toFloat()
            if (w <= 0f) return@post
            val accent = run {
                val tv2 = android.util.TypedValue()
                theme.resolveAttribute(R.attr.accent, tv2, true)
                if (tv2.data != 0) tv2.data else 0xFF4FC3F7.toInt()
            }
            val base = tv.currentTextColor
            val shader = android.graphics.LinearGradient(
                0f, 0f, w * 0.35f, 0f,
                intArrayOf(base, accent, base),
                floatArrayOf(0f, 0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            tv.paint.shader = shader
            val matrix = android.graphics.Matrix()
            val anim = android.animation.ValueAnimator.ofFloat(-w, w * 2f)
            anim.duration = 1500L
            anim.startDelay = 650L
            anim.addUpdateListener {
                matrix.setTranslate(it.animatedValue as Float, 0f)
                shader.setLocalMatrix(matrix)
                tv.invalidate()
            }
            anim.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    tv.paint.shader = null; tv.invalidate()
                }
            })
            anim.start()
        }
    }

    /** Time-of-day greeting (no name), e.g. "مساء الخير" / "Good evening". */
    private fun timeGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return getString(
            when {
                hour < 12 -> R.string.greet_morning
                hour < 18 -> R.string.greet_afternoon
                else -> R.string.greet_evening
            }
        )
    }

    /** Ask once for coarse location so the weather can follow the car's real position. */
    private fun ensureLocationPermission() {
        try {
            if (LocationHelper.hasPermission(this)) {
                LocationHelper.refresh(this)
                return
            }
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQ_LOCATION
            )
        } catch (e: Throwable) {
            // Never let the optional permission flow crash HOME.
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION &&
            grantResults.any { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
        ) {
            LocationHelper.refresh(this)
        }
    }

    private fun showThemePicker() {
        val names = arrayOf(
            getString(R.string.theme_blue), getString(R.string.theme_gold),
            getString(R.string.theme_green), getString(R.string.theme_purple),
            getString(R.string.theme_red), getString(R.string.theme_turquoise),
            getString(R.string.theme_orange), getString(R.string.theme_pink),
            getString(R.string.theme_silver)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.theme_pick)
            .setItems(names) { _, which ->
                Prefs.setThemeIndex(this, which)
                recreate()
            }
            .show()
    }

    private fun showWallpaperMenu() {
        val items = arrayOf(
            getString(R.string.theme_color),
            getString(R.string.bg_style),
            getString(R.string.name_prompt),
            getString(R.string.language),
            getString(R.string.bg_from_gallery),
            getString(R.string.bg_from_url),
            getString(R.string.bg_reset)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.personalize)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showThemePicker()
                    1 -> showBgStylePicker()
                    2 -> showNameDialog()
                    3 -> showLanguageDialog()
                    4 -> pickImage.launch(arrayOf("image/*"))
                    5 -> showUrlDialog()
                    6 -> { Prefs.setBgUri(this, null); applyBackground() }
                }
            }
            .show()
    }

    private fun showNameDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.name_hint)
        input.setText(Prefs.userName(this))
        AlertDialog.Builder(this)
            .setTitle(R.string.name_prompt)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                Prefs.setUserName(this, input.text.toString().trim())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLanguageDialog() {
        val names = arrayOf(getString(R.string.lang_arabic), getString(R.string.lang_english))
        AlertDialog.Builder(this)
            .setTitle(R.string.language)
            .setItems(names) { _, which ->
                val tag = if (which == 0) "ar" else "en"
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.forLanguageTags(tag)
                )
            }
            .show()
    }

    private fun showBgStylePicker() {
        val names = arrayOf(
            getString(R.string.style_aurora),
            getString(R.string.style_waves),
            getString(R.string.style_particles),
            getString(R.string.style_grid),
            getString(R.string.style_rings)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.bg_style)
            .setItems(names) { _, which ->
                Prefs.setBgStyle(this, which)
                b.aurora.style = which
            }
            .show()
    }

    private fun showUrlDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.bg_url_hint)
        AlertDialog.Builder(this)
            .setTitle(R.string.bg_from_url)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val url = input.text.toString().trim()
                if (url.startsWith("http")) downloadBackground(url)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun downloadBackground(url: String) {
        Toast.makeText(this, R.string.downloading, Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000; readTimeout = 15000; instanceFollowRedirects = true
                }
                val bytes = conn.inputStream.use { it.readBytes() }
                conn.disconnect()
                val file = File(filesDir, "zarwa_bg.jpg")
                file.writeBytes(bytes)
                runOnUiThread {
                    Prefs.setBgUri(this, Uri.fromFile(file).toString())
                    applyBackground()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, R.string.download_failed, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun applyBackground() {
        val uriStr = Prefs.bgUri(this)
        if (uriStr == null) {
            b.imgBackground.visibility = View.GONE
            b.bgScrim.visibility = View.GONE
            return
        }
        try {
            contentResolver.openInputStream(Uri.parse(uriStr)).use { input ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                val bmp = BitmapFactory.decodeStream(input, null, opts)
                if (bmp != null) {
                    b.imgBackground.setImageBitmap(bmp)
                    b.imgBackground.visibility = View.VISIBLE
                    b.bgScrim.visibility = View.VISIBLE
                } else {
                    b.imgBackground.visibility = View.GONE
                    b.bgScrim.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Prefs.setBgUri(this, null)
            b.imgBackground.visibility = View.GONE
            b.bgScrim.visibility = View.GONE
        }
    }

    private fun setImmersive() {
        // Keep it minimal — don't hide the navigation bar. Aggressive immersive
        // flags can restart SystemUI (or the whole unit) on locked head units.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun buildDots(count: Int) {
        b.dots.removeAllViews()
        repeat(count) {
            val dot = ImageView(this)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginStart = 6; lp.marginEnd = 6
            dot.layoutParams = lp
            b.dots.addView(dot)
        }
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until b.dots.childCount) {
            (b.dots.getChildAt(i) as ImageView).setImageResource(
                if (i == selected) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }

    override fun onBackPressed() {
        if (b.pager.currentItem != 0) b.pager.currentItem = 0
    }
}

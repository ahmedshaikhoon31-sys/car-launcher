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

        b.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int) = when (position) {
                0 -> DashboardFragment()
                1 -> CustomFragment()
                else -> AppsFragment()
            }
        }
        b.pager.offscreenPageLimit = 1

        buildDots(3)
        b.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateDots(position)
        })
        updateDots(0)

        b.btnTheme.setOnClickListener {
            Prefs.setDark(this, !Prefs.isDark(this)) // day/night toggle (triggers recreate)
        }
        b.btnTheme.setOnLongClickListener { showThemePicker(); true }

        b.btnWall.setOnClickListener { showWallpaperMenu() }
    }

    private fun showThemePicker() {
        val names = arrayOf(
            getString(R.string.theme_blue), getString(R.string.theme_gold),
            getString(R.string.theme_green), getString(R.string.theme_purple),
            getString(R.string.theme_red)
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
            getString(R.string.bg_from_gallery),
            getString(R.string.bg_from_url),
            getString(R.string.bg_reset)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.bg_choose)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> pickImage.launch(arrayOf("image/*"))
                    1 -> showUrlDialog()
                    2 -> { Prefs.setBgUri(this, null); applyBackground() }
                }
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

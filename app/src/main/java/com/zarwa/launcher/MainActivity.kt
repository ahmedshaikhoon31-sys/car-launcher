package com.zarwa.launcher

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.zarwa.launcher.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.applyTheme(Prefs.isDark(this))
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setImmersive()

        b.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int) =
                if (position == 0) DashboardFragment() else AppsFragment()
        }
        b.pager.offscreenPageLimit = 1

        buildDots(2)
        b.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateDots(position)
        })
        updateDots(0)

        b.btnTheme.setOnClickListener {
            Prefs.setDark(this, !Prefs.isDark(this)) // triggers recreate via night mode
        }
    }

    private fun setImmersive() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
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
        // else stay on home (launcher must not exit to black)
    }
}

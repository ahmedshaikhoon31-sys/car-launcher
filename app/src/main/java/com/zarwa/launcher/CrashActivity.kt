package com.zarwa.launcher

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Shows a crash trace full-screen so the user can screenshot and send it. */
class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra("trace") ?: "no trace"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B1220"))
            setPadding(40, 40, 40, 40)
        }
        root.addView(TextView(this).apply {
            text = "‏حصل خطأ في ZARWA — صوّر الشاشة دي وابعتها"
            setTextColor(Color.parseColor("#4FC3F7"))
            textSize = 18f
            gravity = Gravity.CENTER
        })
        val scroll = ScrollView(this)
        scroll.addView(TextView(this).apply {
            text = trace
            setTextColor(Color.parseColor("#EAF2FB"))
            textSize = 12f
            setPadding(0, 24, 0, 0)
            setTextIsSelectable(true)
        })
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ))
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}

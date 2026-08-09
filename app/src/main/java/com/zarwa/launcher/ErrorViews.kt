package com.zarwa.launcher

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Builds a readable error view from a Throwable so a failing page shows the
 * problem inline instead of taking the whole launcher down to a black screen.
 * Also appends the trace to crash.txt for later retrieval.
 */
fun errorView(ctx: Context, where: String, e: Throwable): View {
    val sw = StringWriter()
    e.printStackTrace(PrintWriter(sw))
    val text = "ZARWA — خطأ في $where\n\n$sw"
    try {
        File(ctx.getExternalFilesDir(null), "crash.txt").appendText("\n\n$text")
    } catch (_: Exception) {
    }
    val tv = TextView(ctx).apply {
        setText(text)
        setTextColor(Color.parseColor("#EAF2FB"))
        textSize = 12f
        setPadding(40, 40, 40, 40)
        setTextIsSelectable(true)
    }
    return ScrollView(ctx).apply {
        setBackgroundColor(Color.parseColor("#0B1220"))
        addView(tv)
    }
}

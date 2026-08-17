package com.zarwa.launcher

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Optional global font override. Choice 0 keeps each screen's hand-tuned design
 * fonts; choices 1..4 apply one family everywhere (family only — each view keeps
 * its own bold/italic style).
 */
object FontUtil {

    fun choice(ctx: Context): Int = Prefs.fontChoice(ctx)

    private fun typefaceFor(choice: Int): Typeface? = when (choice) {
        1 -> Typeface.create("sans-serif", Typeface.NORMAL)
        2 -> Typeface.create("sans-serif-light", Typeface.NORMAL)
        3 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
        4 -> Typeface.create("serif", Typeface.NORMAL)
        else -> null // 0 = keep the original design fonts
    }

    /** Applies the user's chosen font to every TextView under [root]. Safe no-op on choice 0. */
    fun apply(ctx: Context, root: View) {
        val tf = typefaceFor(Prefs.fontChoice(ctx)) ?: return
        walk(root, tf)
    }

    private fun walk(v: View, tf: Typeface) {
        when (v) {
            is TextView -> {
                val style = v.typeface?.style ?: Typeface.NORMAL
                v.setTypeface(tf, style)
            }
            is ViewGroup -> for (i in 0 until v.childCount) walk(v.getChildAt(i), tf)
        }
    }
}

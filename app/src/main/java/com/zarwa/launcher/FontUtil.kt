package com.zarwa.launcher

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Optional global font override: family, weight and size. All default to the
 * original hand-tuned design; each option can be changed independently.
 */
object FontUtil {

    private fun familyFor(choice: Int): String? = when (choice) {
        1 -> "sans-serif"
        2 -> "sans-serif-light"
        3 -> "sans-serif-medium"
        4 -> "serif"
        else -> null // 0 = keep the design family
    }

    private fun weightFor(choice: Int): Int = when (choice) {
        1 -> 300 // thin
        2 -> 400 // regular
        3 -> 500 // medium
        4 -> 700 // bold
        else -> 0 // 0 = keep the design weight
    }

    private fun scaleFor(choice: Int): Float = when (choice) {
        0 -> 0.9f  // small
        2 -> 1.15f // large
        else -> 1f // normal
    }

    /** Applies the user's font family/weight/size to every TextView under [root]. */
    fun apply(ctx: Context, root: View) {
        val family = familyFor(Prefs.fontChoice(ctx))
        val weight = weightFor(Prefs.fontWeight(ctx))
        val scale = scaleFor(Prefs.fontScale(ctx))
        if (family == null && weight == 0 && scale == 1f) return
        walk(root, family, weight, scale)
    }

    private fun walk(v: View, family: String?, weight: Int, scale: Float) {
        when (v) {
            is TextView -> applyTo(v, family, weight, scale)
            is ViewGroup -> for (i in 0 until v.childCount) walk(v.getChildAt(i), family, weight, scale)
        }
    }

    private fun applyTo(tv: TextView, family: String?, weight: Int, scale: Float) {
        // family
        var tf: Typeface? = if (family != null) Typeface.create(family, Typeface.NORMAL) else tv.typeface
        // weight (precise on API 28+, else fall back to bold/normal style)
        if (weight != 0) {
            tf = if (Build.VERSION.SDK_INT >= 28) {
                Typeface.create(tf ?: Typeface.DEFAULT, weight, tv.typeface?.isItalic ?: false)
            } else {
                Typeface.create(tf ?: Typeface.DEFAULT, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
            }
            tv.typeface = tf
        } else if (family != null) {
            tv.setTypeface(tf, tv.typeface?.style ?: Typeface.NORMAL)
        }
        // size
        if (scale != 1f) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.textSize * scale)
        }
    }
}

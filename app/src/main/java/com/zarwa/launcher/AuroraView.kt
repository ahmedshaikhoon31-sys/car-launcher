package com.zarwa.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * A subtle living background: two soft light blobs drift slowly to give the
 * launcher a premium "alive" feel without clutter or heavy cost. Shaders are
 * built once per size; only the canvas is translated per frame.
 */
class AuroraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var blobA: RadialGradient? = null
    private var blobB: RadialGradient? = null
    private var radius = 0f
    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration = 26000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { phase = it.animatedValue as Float; invalidate() }
    }

    // MBUX-style turquoise + deep blue drifting glows
    private fun colorA(): Int = if (isNight()) 0x3A22D8C4.toInt() else 0x2422D8C4
    private fun colorB(): Int = if (isNight()) 0x2E2E8BD6.toInt() else 0x1E2E8BD6

    private fun isNight(): Boolean =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = maxOf(w, h) * 0.55f
        blobA = RadialGradient(0f, 0f, radius, colorA(), 0x00000000, Shader.TileMode.CLAMP)
        blobB = RadialGradient(0f, 0f, radius, colorB(), 0x00000000, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val a = blobA ?: return
        val b = blobB ?: return

        val ax = w * (0.22f + 0.10f * sin(phase))
        val ay = h * (0.28f + 0.10f * cos(phase * 0.8f))
        canvas.save()
        canvas.translate(ax, ay)
        paint.shader = a
        canvas.drawCircle(0f, 0f, radius, paint)
        canvas.restore()

        val bx = w * (0.82f + 0.10f * cos(phase * 0.9f))
        val by = h * (0.82f + 0.08f * sin(phase))
        canvas.save()
        canvas.translate(bx, by)
        paint.shader = b
        canvas.drawCircle(0f, 0f, radius, paint)
        canvas.restore()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}

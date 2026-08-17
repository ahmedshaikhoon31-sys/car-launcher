package com.zarwa.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * A subtle living background with 3 styles the user can pick:
 *   0 = Aurora  (soft drifting glows)
 *   1 = Waves   (flowing accent lines)
 *   2 = Particles (gentle floating dots)
 * All are tinted with the active theme's accent colour and kept light on CPU.
 */
class AuroraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var style: Int = 0
        set(value) { field = value; buildBlobs(); invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var blobA: RadialGradient? = null
    private var blobB: RadialGradient? = null
    private var radius = 0f
    private var phase = 0f
    private val path = Path()

    private class Dot(var x: Float, var y: Float, var s: Float, var sp: Float)
    private val dots = ArrayList<Dot>()

    private val animator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration = 26000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { phase = it.animatedValue as Float; invalidate() }
    }

    private fun isNight(): Boolean =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun accent(): Int {
        val tv = android.util.TypedValue()
        context.theme.resolveAttribute(R.attr.accent, tv, true)
        return if (tv.data != 0) tv.data else 0xFF4FC3F7.toInt()
    }

    private fun withAlpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)

    private fun buildBlobs() {
        if (width == 0) return
        radius = maxOf(width, height) * 0.55f
        val ca = withAlpha(accent(), if (isNight()) 0x38 else 0x24)
        val cb = withAlpha(accent(), if (isNight()) 0x20 else 0x14)
        blobA = RadialGradient(0f, 0f, radius, ca, 0x00000000, Shader.TileMode.CLAMP)
        blobB = RadialGradient(0f, 0f, radius, cb, 0x00000000, Shader.TileMode.CLAMP)
    }

    private fun buildDots() {
        dots.clear()
        val rnd = java.util.Random(42)
        repeat(26) {
            dots.add(Dot(rnd.nextFloat(), rnd.nextFloat(), 1.5f + rnd.nextFloat() * 2.5f, 0.4f + rnd.nextFloat()))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildBlobs()
        buildDots()
    }

    override fun onDraw(canvas: Canvas) {
        when (style) {
            1 -> drawWaves(canvas)
            2 -> drawParticles(canvas)
            3 -> drawGrid(canvas)
            4 -> drawRings(canvas)
            else -> drawAurora(canvas)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * resources.displayMetrics.density
        paint.color = withAlpha(accent(), 0x16)
        val spacing = 66f * resources.displayMetrics.density
        val off = (phase / (2 * Math.PI).toFloat()) * spacing
        var x = -spacing + off % spacing
        while (x <= w) { canvas.drawLine(x, 0f, x, h, paint); x += spacing }
        var y = -spacing + off % spacing
        while (y <= h) { canvas.drawLine(0f, y, w, y, paint); y += spacing }
        paint.style = Paint.Style.FILL
    }

    private fun drawRings(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * resources.displayMetrics.density
        val cx = w * 0.2f; val cy = h * 0.3f
        val maxR = maxOf(w, h) * 0.7f
        val base = phase / (2 * Math.PI).toFloat()
        for (i in 0 until 4) {
            var t = (base + i * 0.25f) % 1f
            if (t < 0) t += 1f
            paint.color = withAlpha(accent(), (0x3C * (1f - t)).toInt().coerceAtLeast(0))
            canvas.drawCircle(cx, cy, t * maxR, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawAurora(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val a = blobA ?: return
        val b = blobB ?: return
        canvas.save(); canvas.translate(w * (0.22f + 0.10f * sin(phase)), h * (0.28f + 0.10f * cos(phase * 0.8f)))
        paint.shader = a; canvas.drawCircle(0f, 0f, radius, paint); canvas.restore()
        canvas.save(); canvas.translate(w * (0.82f + 0.10f * cos(phase * 0.9f)), h * (0.82f + 0.08f * sin(phase)))
        paint.shader = b; canvas.drawCircle(0f, 0f, radius, paint); canvas.restore()
    }

    private fun drawWaves(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * resources.displayMetrics.density
        val amp = h * 0.05f
        for (i in 0 until 3) {
            paint.color = withAlpha(accent(), 0x2E - i * 8)
            path.reset()
            val baseY = h * (0.45f + i * 0.2f)
            var x = 0f
            path.moveTo(0f, baseY)
            while (x <= w) {
                val y = baseY + amp * sin(x * 0.004f + phase * 1.5f + i)
                path.lineTo(x, y)
                x += 14f
            }
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawParticles(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        paint.shader = null
        val d = resources.displayMetrics.density
        val prog = phase / (2 * Math.PI).toFloat()
        for (dot in dots) {
            var y = (dot.y - prog * dot.sp * 0.5f) % 1f
            if (y < 0) y += 1f
            val x = (dot.x + 0.02f * sin(phase + dot.y * 6f)) * w
            paint.color = withAlpha(accent(), (0x50 * (0.4f + 0.6f * dot.sp)).toInt().coerceAtMost(0x70))
            canvas.drawCircle(x, y * h, dot.s * d, paint)
        }
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

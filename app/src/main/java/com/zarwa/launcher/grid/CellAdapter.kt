package com.zarwa.launcher.grid

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zarwa.launcher.R

/**
 * Renders the customizable grid cells. Content for each cell is built
 * programmatically so the same container works for apps and every widget type.
 */
class CellAdapter(
    private val ctx: Context,
    private val cells: MutableList<Cell>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<CellAdapter.VH>() {

    var cellHeightPx = 0

    // Live values pushed from the fragment each tick.
    var timeText = "--:--"
    var weatherTemp = "--°"
    var weatherIcon = R.drawable.ic_cloud
    var mediaTitle = ""
    var mediaPlaying = false
    var speedText = "0"

    private val iconCache = HashMap<String, Drawable?>()
    private val labelCache = HashMap<String, String>()

    inner class VH(val frame: FrameLayout) : RecyclerView.ViewHolder(frame)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cell, parent, false) as FrameLayout
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cell = cells[position]
        val root = holder.frame
        if (cellHeightPx > 0) {
            val lp = root.layoutParams
            lp.height = cellHeightPx
            root.layoutParams = lp
        }
        root.removeAllViews()
        root.addView(buildContent(cell))
        root.setOnClickListener { onClick(holder.bindingAdapterPosition) }
    }

    override fun getItemCount() = cells.size

    private fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    private fun buildContent(cell: Cell): View = when (cell.type) {
        Cell.CLOCK -> centered(text(timeText, 34f, R.color.text_primary))
        Cell.WEATHER -> column(icon(weatherIcon, 40), text(weatherTemp, 24f, R.color.text_primary))
        Cell.MEDIA -> {
            val t = if (mediaTitle.isBlank()) ctx.getString(R.string.now_playing) else mediaTitle
            column(
                icon(if (mediaPlaying) R.drawable.ic_pause else R.drawable.ic_play, 34),
                text(t, 14f, R.color.text_secondary).apply { maxLines = 1 }
            )
        }
        Cell.NAV -> column(icon(R.drawable.ic_maps, 40), text(ctx.getString(R.string.maps), 15f, R.color.text_secondary))
        Cell.SPEED -> column(
            text(speedText, 40f, R.color.text_primary),
            text(ctx.getString(R.string.speed_unit), 13f, R.color.text_secondary)
        )
        Cell.APP -> {
            val pkg = cell.pkg
            if (pkg == null) hint()
            else column(appIcon(pkg, 46), text(appLabel(pkg), 14f, R.color.text_primary).apply { maxLines = 1 })
        }
        else -> hint()
    }

    private fun hint(): View = centered(
        text("+", 34f, R.color.text_muted)
    )

    private fun centered(child: View): View {
        val f = FrameLayout(ctx)
        f.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        child.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        )
        f.addView(child)
        return f
    }

    private fun column(vararg children: View): View {
        val ll = LinearLayout(ctx)
        ll.orientation = LinearLayout.VERTICAL
        ll.gravity = Gravity.CENTER
        ll.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        children.forEachIndexed { i, c ->
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (i > 0) lp.topMargin = dp(8)
            c.layoutParams = lp
            ll.addView(c)
        }
        return ll
    }

    private fun text(str: String, sizeSp: Float, colorRes: Int): TextView {
        val t = TextView(ctx)
        t.text = str
        t.textSize = sizeSp
        t.setTextColor(ContextCompat.getColor(ctx, colorRes))
        t.gravity = Gravity.CENTER
        t.ellipsize = android.text.TextUtils.TruncateAt.END
        return t
    }

    private fun icon(res: Int, sizeDp: Int): ImageView {
        val iv = ImageView(ctx)
        iv.setImageResource(res)
        iv.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        return iv
    }

    private fun appIcon(pkg: String, sizeDp: Int): ImageView {
        val d = iconCache.getOrPut(pkg) {
            try { ctx.packageManager.getApplicationIcon(pkg) } catch (e: Exception) { null }
        }
        val iv = ImageView(ctx)
        if (d != null) iv.setImageDrawable(d) else iv.setImageResource(R.drawable.ic_apps)
        iv.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        return iv
    }

    private fun appLabel(pkg: String): String = labelCache.getOrPut(pkg) {
        try {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) { pkg }
    }
}

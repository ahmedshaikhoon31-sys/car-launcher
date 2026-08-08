package com.zarwa.launcher.grid

import android.content.Context
import com.zarwa.launcher.Prefs
import org.json.JSONArray
import org.json.JSONObject

/** One tile in the customizable grid. */
data class Cell(var type: String, var pkg: String? = null) {
    companion object {
        const val EMPTY = "empty"
        const val APP = "app"
        const val CLOCK = "clock"
        const val WEATHER = "weather"
        const val MEDIA = "media"
        const val NAV = "nav"
    }
}

/** Persists the grid layout (cell count + each cell's content). */
object GridStore {

    fun load(ctx: Context): Pair<Int, MutableList<Cell>> {
        val raw = Prefs.gridConfig(ctx)
        if (raw.isBlank()) return defaultConfig()
        return try {
            val obj = JSONObject(raw)
            val count = obj.optInt("count", 4)
            val arr = obj.getJSONArray("cells")
            val cells = ArrayList<Cell>()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                cells.add(Cell(c.optString("t", Cell.EMPTY), c.optString("p", null).ifBlankNull()))
            }
            normalize(count, cells)
            count to cells
        } catch (e: Exception) {
            defaultConfig()
        }
    }

    fun save(ctx: Context, count: Int, cells: List<Cell>) {
        val arr = JSONArray()
        cells.forEach { c ->
            val o = JSONObject()
            o.put("t", c.type)
            if (c.pkg != null) o.put("p", c.pkg)
            arr.put(o)
        }
        val obj = JSONObject()
        obj.put("count", count)
        obj.put("cells", arr)
        Prefs.setGridConfig(ctx, obj.toString())
    }

    private fun defaultConfig(): Pair<Int, MutableList<Cell>> {
        val cells = mutableListOf(
            Cell(Cell.CLOCK), Cell(Cell.WEATHER), Cell(Cell.MEDIA), Cell(Cell.NAV)
        )
        return 4 to cells
    }

    /** Make the cell list exactly [count] long (pad with empties / trim). */
    fun normalize(count: Int, cells: MutableList<Cell>) {
        while (cells.size < count) cells.add(Cell(Cell.EMPTY))
        while (cells.size > count) cells.removeAt(cells.size - 1)
    }

    private fun String?.ifBlankNull(): String? =
        if (this.isNullOrBlank() || this == "null") null else this
}

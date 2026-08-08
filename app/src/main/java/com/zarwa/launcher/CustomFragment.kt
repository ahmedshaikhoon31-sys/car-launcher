package com.zarwa.launcher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.zarwa.launcher.databinding.FragmentCustomBinding
import com.zarwa.launcher.grid.Cell
import com.zarwa.launcher.grid.CellAdapter
import com.zarwa.launcher.grid.GridStore
import com.zarwa.launcher.media.MediaHub
import com.zarwa.launcher.weather.WeatherRepo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomFragment : Fragment() {

    private var _b: FragmentCustomBinding? = null
    private val b get() = _b!!

    private val handler = Handler(Looper.getMainLooper())
    private val timeFmt = SimpleDateFormat("h:mm", Locale.US)
    private var count = 4
    private lateinit var cells: MutableList<Cell>
    private var adapter: CellAdapter? = null
    private var editMode = false
    private var lastWeather = 0L

    private val tick = object : Runnable {
        override fun run() {
            val a = adapter
            val ctx = context
            if (a != null && ctx != null) {
                a.timeText = timeFmt.format(Date())
                val np = if (MediaHub.hasNotificationAccess(ctx)) MediaHub.nowPlaying(ctx) else null
                a.mediaTitle = np?.title ?: ""
                a.mediaPlaying = np?.isPlaying ?: false
                a.notifyDataSetChanged()
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentCustomBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        val cfg = GridStore.load(ctx)
        count = cfg.first
        cells = cfg.second

        adapter = CellAdapter(ctx, cells) { pos -> onCellClick(pos) }
        b.gridCells.layoutManager = GridLayoutManager(ctx, count / 2)
        b.gridCells.adapter = adapter
        recomputeCellHeight()

        b.btnEdit.setOnClickListener {
            editMode = !editMode
            b.editHint.visibility = if (editMode) View.VISIBLE else View.GONE
        }
        b.btnSize4.setOnClickListener { setSize(4) }
        b.btnSize6.setOnClickListener { setSize(6) }
        b.btnSize8.setOnClickListener { setSize(8) }

        fetchWeather()
    }

    private fun recomputeCellHeight() {
        b.gridCells.post {
            val a = adapter ?: return@post
            val h = b.gridCells.height
            if (h > 0) {
                a.cellHeightPx = h / 2
                a.notifyDataSetChanged()
            }
        }
    }

    private fun setSize(newCount: Int) {
        count = newCount
        GridStore.normalize(count, cells)
        GridStore.save(requireContext(), count, cells)
        (b.gridCells.layoutManager as GridLayoutManager).spanCount = count / 2
        recomputeCellHeight()
        adapter?.notifyDataSetChanged()
    }

    private fun onCellClick(pos: Int) {
        if (pos < 0 || pos >= cells.size) return
        if (editMode) {
            showChooser(pos)
        } else {
            activate(cells[pos])
        }
    }

    private fun activate(cell: Cell) {
        val ctx = context ?: return
        when (cell.type) {
            Cell.APP -> cell.pkg?.let { AppLauncher.launchPackage(ctx, it) }
            Cell.MEDIA -> MediaHub.playPause(ctx)
            Cell.NAV -> AppLauncher.openMaps(ctx)
            else -> {}
        }
    }

    private fun showChooser(pos: Int) {
        val ctx = requireContext()
        val labels = arrayOf(
            getString(R.string.cell_app), getString(R.string.cell_clock),
            getString(R.string.cell_weather), getString(R.string.cell_media),
            getString(R.string.cell_nav), getString(R.string.cell_empty)
        )
        AlertDialog.Builder(ctx)
            .setTitle(R.string.choose_content)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> showAppPicker(pos)
                    1 -> setCell(pos, Cell.CLOCK)
                    2 -> setCell(pos, Cell.WEATHER)
                    3 -> setCell(pos, Cell.MEDIA)
                    4 -> setCell(pos, Cell.NAV)
                    5 -> setCell(pos, Cell.EMPTY)
                }
            }
            .show()
    }

    private fun showAppPicker(pos: Int) {
        val ctx = requireContext()
        AppRepository.load(ctx) { apps ->
            if (_b == null) return@load
            val labels = apps.map { it.label }.toTypedArray()
            AlertDialog.Builder(ctx)
                .setTitle(R.string.choose_app)
                .setItems(labels) { _, which ->
                    val app = apps[which]
                    cells[pos] = Cell(Cell.APP, app.packageName)
                    persistAndRefresh()
                }
                .show()
        }
    }

    private fun setCell(pos: Int, type: String) {
        cells[pos] = Cell(type)
        persistAndRefresh()
    }

    private fun persistAndRefresh() {
        GridStore.save(requireContext(), count, cells)
        adapter?.notifyDataSetChanged()
    }

    private fun fetchWeather() {
        val now = System.currentTimeMillis()
        if (now - lastWeather < 15 * 60 * 1000L && lastWeather != 0L) return
        lastWeather = now
        val ctx = context ?: return
        WeatherRepo.fetch(Prefs.lat(ctx), Prefs.lon(ctx)) { w ->
            val a = adapter ?: return@fetch
            if (w == null) return@fetch
            a.weatherTemp = "${w.tempC}°"
            a.weatherIcon = w.iconRes
            a.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
        fetchWeather()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

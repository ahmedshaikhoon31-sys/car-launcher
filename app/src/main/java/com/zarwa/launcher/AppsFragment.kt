package com.zarwa.launcher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.zarwa.launcher.databinding.FragmentAppsBinding

class AppsFragment : Fragment() {

    private var _b: FragmentAppsBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: AppsAdapter
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentAppsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        adapter = AppsAdapter { app -> AppLauncher.launchPackage(ctx, app.packageName) }
        b.gridApps.layoutManager = GridLayoutManager(ctx, 6)
        b.gridApps.adapter = adapter
        b.gridApps.setHasFixedSize(true)

        b.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b2: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b2: Int, c: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        AppRepository.load(ctx) { list ->
            allApps = list
            _b?.let { adapter.submit(list) }
        }
    }

    private fun filter(query: String) {
        val q = query.trim().lowercase()
        adapter.submit(
            if (q.isEmpty()) allApps else allApps.filter { it.label.lowercase().contains(q) }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}

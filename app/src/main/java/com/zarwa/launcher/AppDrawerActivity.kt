package com.zarwa.launcher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.zarwa.launcher.databinding.ActivityAppDrawerBinding

class AppDrawerActivity : AppCompatActivity() {

    private lateinit var b: ActivityAppDrawerBinding
    private lateinit var adapter: AppsAdapter
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAppDrawerBinding.inflate(layoutInflater)
        setContentView(b.root)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        adapter = AppsAdapter { app ->
            AppLauncher.launchPackage(this, app.packageName)
        }
        b.gridApps.layoutManager = GridLayoutManager(this, 5)
        b.gridApps.adapter = adapter
        b.gridApps.setHasFixedSize(true)

        b.btnBack.setOnClickListener { finish() }

        b.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadApps()
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(intent, 0)
            val myPkg = packageName
            val list = resolved.asSequence()
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != myPkg }
                .mapNotNull { pkg ->
                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        AppInfo(
                            label = pm.getApplicationLabel(appInfo).toString(),
                            packageName = pkg,
                            icon = pm.getApplicationIcon(appInfo)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedBy { it.label.lowercase() }
                .toList()
            runOnUiThread {
                allApps = list
                adapter.submit(list)
            }
        }.start()
    }

    private fun filter(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) allApps
        else allApps.filter { it.label.lowercase().contains(q) }
        adapter.submit(filtered)
    }
}

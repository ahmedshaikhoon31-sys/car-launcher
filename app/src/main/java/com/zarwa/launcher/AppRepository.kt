package com.zarwa.launcher

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

/** Loads the list of launchable apps off the main thread. */
object AppRepository {

    private val main = Handler(Looper.getMainLooper())

    fun load(ctx: Context, cb: (List<AppInfo>) -> Unit) {
        val appCtx = ctx.applicationContext
        Thread {
            val pm = appCtx.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(intent, 0)
            val myPkg = appCtx.packageName
            val list = resolved.asSequence()
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != myPkg }
                .mapNotNull { pkg ->
                    try {
                        val info = pm.getApplicationInfo(pkg, 0)
                        AppInfo(
                            label = pm.getApplicationLabel(info).toString(),
                            packageName = pkg,
                            icon = pm.getApplicationIcon(info)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedBy { it.label.lowercase() }
                .toList()
            main.post { cb(list) }
        }.start()
    }
}

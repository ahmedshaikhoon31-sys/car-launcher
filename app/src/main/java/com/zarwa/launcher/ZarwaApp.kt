package com.zarwa.launcher

import android.app.Application
import android.content.Intent
import android.os.Process
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Catches any uncaught crash and shows it on a readable screen (and saves it to
 * a file) instead of the launcher just dying to a black screen. Makes debugging
 * on the head unit possible without a computer.
 */
class ZarwaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Apply the day/night mode once, before any activity, so the launcher never
        // recreates itself on first launch (which would swallow the welcome screen).
        Prefs.applyTheme(Prefs.isDark(this))
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val trace = buildString {
                append("ZARWA crash\n\n")
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                append(sw.toString())
            }
            try {
                File(getExternalFilesDir(null), "crash.txt").writeText(trace)
            } catch (_: Exception) {
            }
            try {
                startActivity(
                    Intent(this, CrashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra("trace", trace)
                )
            } catch (_: Exception) {
            }
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }
}

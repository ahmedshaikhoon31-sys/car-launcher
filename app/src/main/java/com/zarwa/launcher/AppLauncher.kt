package com.zarwa.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/**
 * Robust app launching. Head units vary wildly in which packages they ship,
 * so every action tries several strategies and falls back gracefully to the
 * app drawer instead of crashing or doing nothing.
 */
object AppLauncher {

    fun launchPackage(ctx: Context, pkg: String): Boolean {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    /** Try each package in order; return true on the first that launches. */
    private fun launchFirst(ctx: Context, packages: List<String>): Boolean {
        for (p in packages) if (launchPackage(ctx, p)) return true
        return false
    }

    private fun start(ctx: Context, intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun openMainCategory(ctx: Context, category: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return start(ctx, intent)
    }

    // ---- High level actions ----

    fun openMaps(ctx: Context) {
        if (launchFirst(ctx, listOf(
                "com.google.android.apps.maps",
                "com.autonavi.minimap",
                "com.here.app.maps"
            ))) return
        if (start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")))) return
        openDrawer(ctx)
    }

    fun openMusic(ctx: Context) {
        if (openMainCategory(ctx, Intent.CATEGORY_APP_MUSIC)) return
        if (launchFirst(ctx, listOf(
                "com.google.android.apps.youtube.music",
                "com.spotify.music",
                "com.android.music",
                "com.android.mediacenter"
            ))) return
        openDrawer(ctx)
    }

    fun openRadio(ctx: Context) {
        // Common FM-radio packages shipped on Chinese head units.
        if (launchFirst(ctx, listOf(
                "com.android.fmradio",
                "com.hzc.rd",
                "com.microntek.radio",
                "com.zhy.fmradio",
                "com.autochips.radio",
                "com.hct.radio"
            ))) return
        openDrawer(ctx)
    }

    fun openPhone(ctx: Context) {
        if (start(ctx, Intent(Intent.ACTION_DIAL))) return
        openDrawer(ctx)
    }

    fun openBluetoothSettings(ctx: Context) {
        if (start(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))) return
        // Some units bundle a dedicated BT-music app.
        if (launchFirst(ctx, listOf(
                "com.microntek.btphone",
                "com.zhy.btphone",
                "com.autochips.bt"
            ))) return
        openDrawer(ctx)
    }

    fun openVideo(ctx: Context) {
        if (launchFirst(ctx, listOf(
                "com.google.android.youtube",
                "com.android.gallery3d",
                "com.android.video",
                "com.microntek.video"
            ))) return
        if (openMainCategory(ctx, Intent.CATEGORY_APP_GALLERY)) return
        openDrawer(ctx)
    }

    fun openBrowser(ctx: Context) {
        if (openMainCategory(ctx, Intent.CATEGORY_APP_BROWSER)) return
        if (start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))) return
        openDrawer(ctx)
    }

    fun openSettings(ctx: Context) {
        if (start(ctx, Intent(Settings.ACTION_SETTINGS))) return
        openDrawer(ctx)
    }

    fun openCamera(ctx: Context) {
        // Prefer the built-in DVR / rear-camera apps common on head units.
        if (launchFirst(ctx, listOf(
                "com.microntek.dvr",
                "com.avin.dvr",
                "com.autochips.dvr",
                "com.android.camera2",
                "com.android.camera"
            ))) return
        if (start(ctx, Intent("android.media.action.STILL_IMAGE_CAMERA"))) return
        openDrawer(ctx)
    }

    fun openDrawer(ctx: Context) {
        ctx.startActivity(Intent(ctx, AppDrawerActivity::class.java))
    }

    /** Opens the app drawer in "pick an app to open in split view" mode. */
    fun openDrawerForSplit(ctx: Context) {
        ctx.startActivity(
            Intent(ctx, AppDrawerActivity::class.java).putExtra("adjacent", true)
        )
    }

    /**
     * Best-effort split-screen: launch [pkg] adjacent to the current app.
     * Works only if the head unit's Android build supports multi-window;
     * otherwise it just opens normally.
     */
    fun launchAdjacent(ctx: Context, pkg: String): Boolean {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun toast(ctx: Context, msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }
}

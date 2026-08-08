package com.zarwa.launcher.can

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * CAN-bus scaffold. Chinese head units that expose car data broadcast it as
 * Intents, but the action names and extra keys differ per manufacturer. This
 * listens for the most common ones and stores whatever it can parse.
 *
 * On a unit reporting "CAN: Unknown" nothing will arrive until we discover the
 * real action/extra names via `adb logcat` and add them here. When that happens
 * the climate/fuel widgets light up automatically with real values.
 */
object CanBus {
    var climateLeft: Int? = null
    var climateRight: Int? = null
    var rangeKm: Int? = null
    var doorsLocked: Boolean? = null

    // Candidate broadcast actions observed across common head-unit vendors.
    val ACTIONS = listOf(
        "com.microntek.mtcCan",
        "com.microntek.canbus.data",
        "android.intent.action.CANBOX",
        "com.hct.canbus",
        "com.autochips.canbus",
        "com.zhonghong.canbus.data"
    )

    fun intentFilter(): IntentFilter {
        val f = IntentFilter()
        ACTIONS.forEach { f.addAction(it) }
        return f
    }
}

class CanReceiver(private val onUpdate: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val ext = intent?.extras ?: return
        (ext.get("temp_left") ?: ext.get("ac_temp_left") ?: ext.get("tempL")).asIntOrNull()
            ?.let { CanBus.climateLeft = it }
        (ext.get("temp_right") ?: ext.get("ac_temp_right") ?: ext.get("tempR")).asIntOrNull()
            ?.let { CanBus.climateRight = it }
        (ext.get("range") ?: ext.get("fuel_range") ?: ext.get("remain_km")).asIntOrNull()
            ?.let { CanBus.rangeKm = it }
        (ext.get("doors_locked") as? Boolean)?.let { CanBus.doorsLocked = it }
        onUpdate()
    }

    private fun Any?.asIntOrNull(): Int? = when (this) {
        is Number -> toInt()
        is String -> toDoubleOrNull()?.toInt()
        else -> null
    }
}

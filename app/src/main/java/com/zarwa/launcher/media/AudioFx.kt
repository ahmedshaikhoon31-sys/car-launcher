package com.zarwa.launcher.media

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer

/**
 * Process-wide holder for the global audio effects so equalizer settings stay
 * applied after the EQ screen is closed. Session 0 targets the whole output mix,
 * which some head units allow and some don't (handled gracefully via [available]).
 */
object AudioFx {

    var eq: Equalizer? = null
        private set
    var bass: BassBoost? = null
        private set
    var virt: Virtualizer? = null
        private set

    var available = false
        private set

    private var tried = false

    fun ensure() {
        if (tried) return
        tried = true
        try {
            eq = Equalizer(1000, 0).apply { enabled = true }
            available = true
        } catch (e: Throwable) {
            eq = null
        }
        try {
            bass = BassBoost(1000, 0).apply { enabled = true }
        } catch (e: Throwable) {
            bass = null
        }
        try {
            virt = Virtualizer(1000, 0).apply { enabled = true }
        } catch (e: Throwable) {
            virt = null
        }
    }
}

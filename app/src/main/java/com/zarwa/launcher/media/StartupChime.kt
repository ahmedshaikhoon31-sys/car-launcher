package com.zarwa.launcher.media

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * A short, calm premium start-up chime — fully synthesized (no audio files),
 * so there is nothing to license. Plays on a background thread.
 */
object StartupChime {

    private const val SR = 44100

    /** Plays the user's chosen audio file if set, otherwise the synthesized chime. */
    fun playWelcome(ctx: Context) {
        val uri = com.zarwa.launcher.Prefs.chimeUri(ctx)
        if (uri != null) playFile(ctx, uri) else play(ctx, com.zarwa.launcher.Prefs.chime(ctx))
    }

    fun playFile(ctx: Context, uriStr: String) {
        try {
            val mp = android.media.MediaPlayer()
            mp.setDataSource(ctx, android.net.Uri.parse(uriStr))
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { it.release() }
            mp.setOnErrorListener { p, _, _ -> try { p.release() } catch (e: Throwable) {}; true }
            mp.prepareAsync()
        } catch (e: Throwable) {
        }
    }

    fun play(ctx: Context, style: Int) {
        if (style <= 0) return
        Thread {
            try {
                val samples = render(style)
                playBuffer(samples)
            } catch (e: Throwable) {
                // Sound is a nicety — never let it crash anything.
            }
        }.start()
    }

    /** One note = a sine tone with a soft attack and a smooth exponential decay. */
    private fun note(buf: FloatArray, startMs: Int, durMs: Int, freq: Double, gain: Double, attackMs: Int) {
        val start = startMs * SR / 1000
        val len = durMs * SR / 1000
        val attack = (attackMs * SR / 1000).coerceAtLeast(1)
        for (i in 0 until len) {
            val idx = start + i
            if (idx >= buf.size) break
            val env = when {
                i < attack -> i.toDouble() / attack
                else -> exp(-3.0 * (i - attack) / (len - attack).coerceAtLeast(1))
            }
            buf[idx] += (sin(2.0 * PI * freq * i / SR) * env * gain).toFloat()
        }
    }

    private fun render(style: Int): FloatArray {
        return when (style) {
            2 -> { // gentle bell: two clear tones with long decay
                val buf = FloatArray(SR * 2)
                note(buf, 0, 1600, 880.0, 0.5, 4)      // A5
                note(buf, 0, 1600, 880.0 * 2, 0.16, 4) // shimmer harmonic
                note(buf, 260, 1600, 1174.66, 0.42, 4) // D6
                buf
            }
            3 -> { // rising tone: three ascending notes then a held top
                val buf = FloatArray((SR * 1.9).toInt())
                note(buf, 0, 300, 523.25, 0.45, 8)   // C5
                note(buf, 220, 300, 659.25, 0.45, 8) // E5
                note(buf, 440, 1200, 783.99, 0.5, 8) // G5 (held)
                buf
            }
            else -> { // soft whisper: a warm major chord that swells and fades
                val buf = FloatArray((SR * 1.8).toInt())
                note(buf, 0, 1700, 523.25, 0.34, 380) // C5
                note(buf, 0, 1700, 659.25, 0.30, 420) // E5
                note(buf, 0, 1700, 783.99, 0.28, 460) // G5
                buf
            }
        }
    }

    private fun playBuffer(samples: FloatArray) {
        // soft-clip and convert to 16-bit PCM
        val pcm = ShortArray(samples.size)
        for (i in samples.indices) {
            val v = (samples[i] * 0.8f).coerceIn(-1f, 1f)
            pcm[i] = (v * Short.MAX_VALUE).toInt().toShort()
        }
        val at = AudioTrack(
            AudioManager.STREAM_MUSIC, SR,
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
            pcm.size * 2, AudioTrack.MODE_STATIC
        )
        at.write(pcm, 0, pcm.size)
        at.setNotificationMarkerPosition(pcm.size)
        at.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack) {
                try { track.stop(); track.release() } catch (e: Throwable) {}
            }
            override fun onPeriodicNotification(track: AudioTrack) {}
        })
        at.play()
    }
}

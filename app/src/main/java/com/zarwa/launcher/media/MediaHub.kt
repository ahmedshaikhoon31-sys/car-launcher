package com.zarwa.launcher.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.view.KeyEvent

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val isPlaying: Boolean,
    val progress: Float, // 0f..1f, -1 if unknown
    val positionMs: Long = -1L,
    val durationMs: Long = -1L
)

/** Reads and controls whatever media session is currently active on the unit. */
object MediaHub {

    fun hasNotificationAccess(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(
            ctx.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":").any { it.contains(ctx.packageName) }
    }

    private fun controller(ctx: Context): MediaController? {
        return try {
            val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val comp = ComponentName(ctx, ZarwaNotificationListener::class.java)
            msm.getActiveSessions(comp).firstOrNull()
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun nowPlaying(ctx: Context): NowPlaying? {
        val c = controller(ctx) ?: return null
        val md = c.metadata ?: return null
        val state = c.playbackState
        val title = md.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: return null
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val art: Bitmap? = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val playing = state?.state == PlaybackState.STATE_PLAYING
        val dur = md.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val pos = state?.position ?: -1L
        val prog = if (dur > 0 && pos >= 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else -1f
        return NowPlaying(title, artist, art, playing, prog, pos, dur)
    }

    /** Seek to an absolute position (ms). No-op if the session doesn't support it. */
    fun seekTo(ctx: Context, positionMs: Long) {
        try {
            controller(ctx)?.transportControls?.seekTo(positionMs)
        } catch (e: Exception) {
        }
    }

    fun playPause(ctx: Context) {
        val c = controller(ctx)
        if (c != null) {
            if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
            else c.transportControls.play()
        } else sendKey(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun next(ctx: Context) {
        val c = controller(ctx)
        if (c != null) c.transportControls.skipToNext() else sendKey(ctx, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previous(ctx: Context) {
        val c = controller(ctx)
        if (c != null) c.transportControls.skipToPrevious() else sendKey(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    private fun sendKey(ctx: Context, keyCode: Int) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    fun openNotificationAccessSettings(ctx: Context) {
        ctx.startActivity(
            android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

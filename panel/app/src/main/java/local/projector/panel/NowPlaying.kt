package local.projector.panel

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * What is playing right now, from the active media session (SmartTube, VLC, ...).
 * Needs MEDIA_CONTENT_CONTROL (signature), granted by the platform signature.
 * Position is computed from the last playback state update, so it is current at request time.
 *
 * SmartTube only publishes the video id and duration in its metadata; the title and channel
 * come from YouTube's public oEmbed endpoint, cached per video.
 */
class NowPlaying(private val context: Context) {
    private val tracks = mutableMapOf<String, Track>()

    fun status(): String = snapshot(null)

    private fun snapshot(at: Long?): String {
        val c = controller() ?: return """{"available":false}"""
        val playback = c.playbackState
        val meta = c.metadata
        val id = meta?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
        val youtube = if (c.packageName == SMARTTUBE && !id.isNullOrBlank()) track(id) else null
        val art = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: youtube?.art
            ?: ""
        val state = playback?.state ?: PlaybackState.STATE_NONE
        val playing = state == PlaybackState.STATE_PLAYING
        val buffering = state == PlaybackState.STATE_BUFFERING
        val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val speed = playback?.playbackSpeed ?: 0f
        return buildString {
            append("""{"available":true""")
            append(""","package":${json(c.packageName)}""")
            append(""","title":${json(title(meta) ?: youtube?.title ?: id ?: "")}""")
            append(""","artist":${json(artist(meta) ?: youtube?.artist ?: "")}""")
            append(""","art":${json(art)}""")
            append(""","playing":$playing""")
            append(""","buffering":$buffering""")
            append(""","position":${at ?: position(playback)}""")
            append(""","duration":$duration""")
            append(""","speed":$speed}""")
        }
    }

    /** Seeks to an absolute position (ms) or by a relative number of seconds. */
    fun seek(toMs: Long?, bySec: Int?): String {
        require(toMs != null || bySec != null) { "hedef eksik" }
        val c = controller() ?: throw IllegalStateException("şu an oynatılan bir şey yok")
        val duration = c.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        var target = toMs ?: (position(c.playbackState) + (bySec ?: 0) * 1000L)
        target = target.coerceAtLeast(0)
        if (duration > 0) target = target.coerceAtMost(duration)
        c.transportControls.seekTo(target)
        return snapshot(target)
    }

    /** Prefers a playing session; ignores pairing/Bluetooth placeholders. */
    private fun controller(): MediaController? = try {
        val sessions = manager().getActiveSessions(null).filter {
            it.packageName != "com.android.bluetooth" && usable(it)
        }
        sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull()
    } catch (e: Exception) {
        Log.w(TAG, "media sessions not available: ${e.message}")
        null
    }

    private fun manager(): MediaSessionManager =
        context.getSystemService(MediaSessionManager::class.java)

    private fun usable(c: MediaController): Boolean {
        val state = c.playbackState?.state ?: return false
        return state != PlaybackState.STATE_NONE &&
            state != PlaybackState.STATE_STOPPED &&
            state != PlaybackState.STATE_ERROR
    }

    private fun position(playback: PlaybackState?): Long {
        if (playback == null) return 0
        val base = playback.position
        if (playback.state != PlaybackState.STATE_PLAYING || playback.playbackSpeed <= 0f) {
            return base.coerceAtLeast(0)
        }
        val elapsed = SystemClock.elapsedRealtime() - playback.lastPositionUpdateTime
        return (base + (elapsed * playback.playbackSpeed).toLong()).coerceAtLeast(0)
    }

    private fun title(meta: MediaMetadata?): String? =
        meta?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.takeIf { it.isNotBlank() }

    private fun artist(meta: MediaMetadata?): String? =
        meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }

    @Synchronized
    private fun track(id: String): Track? {
        tracks[id]?.let { return it }
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://www.youtube.com/oembed?format=json&url=https://www.youtube.com/watch?v=$id")
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 1500
            conn.readTimeout = 1500
            conn.setRequestProperty("User-Agent", "Panel/0.2")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val o = JSONObject(body)
            Track(o.optString("title"), o.optString("author_name"), o.optString("thumbnail_url")).also {
                if (tracks.size >= 50) tracks.clear()
                tracks[id] = it
            }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube lookup failed for $id: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun json(s: String?): String =
        "\"" + (s ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""

    private data class Track(val title: String, val artist: String, val art: String)

    companion object {
        private const val TAG = "Panel"
        private const val SMARTTUBE = "org.smarttube.stable"
    }
}

package local.projector.panel

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Sleep timer: at the end, pause playback and turn the lamp off.
 * The end time is saved, so the timer survives an app restart (e.g. after a corner apply).
 * Checks the wall clock every few seconds instead of one long sleep.
 */
class SleepTimer(private val context: Context) {
    private val prefs = context.getSharedPreferences("sleep", Context.MODE_PRIVATE)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val ticker = Executors.newSingleThreadScheduledExecutor()

    init {
        val end = endAt()
        if (end != 0L && now() - end > MISSED_LIMIT_MS) clear() // missed long ago, e.g. overnight
        ticker.scheduleWithFixedDelay({ runCatching { tick() } }, TICK_S, TICK_S, TimeUnit.SECONDS)
    }

    @Synchronized
    fun status(): String {
        val end = endAt()
        if (end == 0L) return """{"active":false,"minutes":${lastMinutes()}}"""
        val left = ((end - now()) / 1000).coerceAtLeast(0)
        return """{"active":true,"endsAt":$end,"left":$left,"minutes":${lastMinutes()}}"""
    }

    /** Starts (or restarts) the timer: lamp off in [minutes]. */
    @Synchronized
    fun start(minutes: Int): String {
        require(minutes in 1..MAX_MINUTES) { "minutes must be 1..$MAX_MINUTES" }
        prefs.edit()
            .putLong("endAt", now() + minutes * 60_000L)
            .putInt("minutes", minutes)
            .putBoolean("warned", false)
            .apply()
        return status()
    }

    /** Adds time to a running timer, or starts one. */
    @Synchronized
    fun add(minutes: Int): String {
        require(minutes in 1..MAX_MINUTES) { "minutes must be 1..$MAX_MINUTES" }
        val end = endAt()
        if (end == 0L) return start(minutes)
        val newEnd = minOf(end + minutes * 60_000L, now() + MAX_MINUTES * 60_000L)
        prefs.edit().putLong("endAt", newEnd).putBoolean("warned", false).apply()
        return status()
    }

    /** Starts again with the last chosen length. */
    @Synchronized
    fun restart(): String = start(lastMinutes())

    @Synchronized
    fun cancel(): String {
        clear()
        return status()
    }

    @Synchronized
    private fun tick() {
        val end = endAt()
        if (end == 0L) return
        val left = end - now()
        if (left <= WARN_MS && left > 0 && !prefs.getBoolean("warned", false)) {
            prefs.edit().putBoolean("warned", true).apply()
            toast("Lamp turns off in 1 minute")
        }
        if (left > 0) return
        try {
            pause()
            Lamp.off()
            clear()
            Log.i(TAG, "sleep timer done: paused, lamp off")
        } catch (e: IllegalStateException) {
            Log.w(TAG, "lamp not ready, retry: ${e.message}") // e.g. still warming up
        }
    }

    private fun pause() {
        val t = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0))
    }

    private fun toast(text: String) = main.post { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }

    private fun clear() = prefs.edit().remove("endAt").putBoolean("warned", false).apply()
    private fun endAt() = prefs.getLong("endAt", 0L)
    private fun lastMinutes() = prefs.getInt("minutes", 30)
    private fun now() = System.currentTimeMillis()

    companion object {
        private const val TAG = "Panel"
        private const val TICK_S = 5L
        private const val WARN_MS = 60_000L
        private const val MISSED_LIMIT_MS = 10 * 60_000L
        const val MAX_MINUTES = 12 * 60
    }
}

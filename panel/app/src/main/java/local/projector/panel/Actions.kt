package local.projector.panel

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent

class Actions(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)

    fun run(name: String, params: Map<String, String>): String = when (name) {
        "play" -> media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        "next" -> media(KeyEvent.KEYCODE_MEDIA_NEXT)
        "prev" -> media(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        "vol-up" -> volume(AudioManager.ADJUST_RAISE)
        "vol-down" -> volume(AudioManager.ADJUST_LOWER)
        "mute" -> volume(AudioManager.ADJUST_TOGGLE_MUTE)
        "open" -> open(params["url"].orEmpty())
        "settings" -> launch(Intent(Settings.ACTION_SETTINGS))
        "keystone" -> launch(Intent().setClassName(KEYSTONE_PKG, KEYSTONE_ACTIVITY))
        "lamp" -> """{"lamp":"${Lamp.status()}"}"""
        "lamp-off" -> """{"lamp":"${Lamp.off()}"}"""
        "lamp-on" -> """{"lamp":"${Lamp.on()}"}"""
        "status" -> status()
        else -> throw IllegalArgumentException("unknown action: $name")
    }

    private fun media(code: Int): String {
        val now = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
        return "ok"
    }

    private fun volume(direction: Int): String {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        return status()
    }

    private fun status(): String {
        val v = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val muted = audio.isStreamMute(AudioManager.STREAM_MUSIC)
        return """{"volume":$v,"max":$max,"muted":$muted}"""
    }

    private fun open(url: String): String {
        require(url.startsWith("http://") || url.startsWith("https://")) { "bad url" }
        return launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(SMARTTUBE))
    }

    private fun launch(intent: Intent): String {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "ok"
    }

    companion object {
        const val SMARTTUBE = "org.smarttube.stable"
        const val KEYSTONE_PKG = "com.htc.htcsettings"
        const val KEYSTONE_ACTIVITY = "com.htc.activity.ProjectionActivity3"
    }
}

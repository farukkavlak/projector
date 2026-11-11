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
    private val keystone = Keystone(context)
    private val corners = Corners(context)
    private val timer = SleepTimer(context)
    private val nowPlaying = NowPlaying(context)
    private val systemStatus = SystemStatus(context)
    private val power = Power(context)
    private val apps = Apps(context)

    fun preview(): ByteArray? = Preview.png()

    fun run(name: String, params: Map<String, String>): String = when (name) {
        "play" -> media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        "next" -> media(KeyEvent.KEYCODE_MEDIA_NEXT)
        "prev" -> media(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        "vol-up" -> volume(AudioManager.ADJUST_RAISE)
        "vol-down" -> volume(AudioManager.ADJUST_LOWER)
        "vol-set" -> volumeSet(params["level"]?.toIntOrNull() ?: throw IllegalArgumentException("level missing"))
        "mute" -> volume(AudioManager.ADJUST_TOGGLE_MUTE)
        "open" -> open(params["url"].orEmpty())
        "settings" -> launch(Intent(Settings.ACTION_SETTINGS))
        "keystone" -> launch(Intent().setClassName(KEYSTONE_PKG, KEYSTONE_ACTIVITY))
        "lamp" -> """{"lamp":"${Lamp.status()}"}"""
        "lamp-off" -> { timer.cancel(); """{"lamp":"${Lamp.off()}"}""" }
        "lamp-on" -> """{"lamp":"${Lamp.on()}"}"""
        "timer" -> timer.status()
        "timer-start" -> timer.start(minutes(params))
        "timer-add" -> timer.add(minutes(params))
        "timer-restart" -> timer.restart()
        "timer-cancel" -> timer.cancel()
        "now" -> nowPlaying.status()
        "seek" -> nowPlaying.seek(
            toMs = params["to"]?.toLongOrNull(),
            bySec = params["by"]?.toIntOrNull(),
        )
        "keystone-get" -> keystone.status()
        "corners" -> corners.status()
        "corners-set" -> corners.apply(params)
        "corners-undo" -> corners.undo()
        "corners-factory" -> corners.factory()
        "presets" -> corners.presets()
        "preset-save" -> corners.savePreset(params["name"], params)
        "preset-apply" -> corners.applyPreset(params["name"])
        "preset-delete" -> corners.deletePreset(params["name"])
        "preset-rename" -> corners.renamePreset(params["from"], params["to"])
        "preset-import" -> corners.importPresets(params["data"])
        "system" -> systemStatus.status()
        "key" -> Keys.press(params["name"])
        "text" -> Keys.type(params["value"].orEmpty())
        "text-clear" -> Keys.clear()
        "apps" -> apps.list()
        "app-open" -> apps.open(params["package"])
        "power" -> power.run(params["action"])
        "focus" -> Focus.nudge(params["dir"])
        "led" -> Leds.set(params["name"], params["on"] == "1")
        "auto" -> Auto.set(params["name"], params["on"] == "1")
        "brightness" -> Pq.brightness()?.let { """{"brightness":$it}""" }
            ?: """{"brightness":null}"""
        "brightness-set" -> {
            val v = params["value"]?.toIntOrNull() ?: throw IllegalArgumentException("value missing")
            Pq.setBrightness(v)
            """{"brightness":${Pq.brightness()}}"""
        }
        "color" -> Pq.colorTemperature()?.let { """{"color":$it}""" } ?: """{"color":null}"""
        "color-set" -> {
            val v = params["value"]?.toIntOrNull() ?: throw IllegalArgumentException("value missing")
            Pq.setColorTemperature(v)
            """{"color":${Pq.colorTemperature()}}"""
        }
        "keystone-set" -> keystone.apply(
            params["tilt"]?.toIntOrNull() ?: throw IllegalArgumentException("tilt missing"),
            params["scale"]?.toIntOrNull() ?: throw IllegalArgumentException("scale missing"),
        )
        "status" -> status()
        else -> throw IllegalArgumentException("unknown action: $name")
    }

    private fun minutes(params: Map<String, String>) =
        params["minutes"]?.toIntOrNull() ?: throw IllegalArgumentException("minutes missing")

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

    private fun volumeSet(level: Int): String {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, level.coerceIn(0, max), 0)
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

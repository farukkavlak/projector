package local.projector.panel

import android.content.Context
import kotlin.concurrent.thread

/**
 * Free 4-corner keystone. SurfaceFlinger reads persist.display.keystone_* only at start,
 * so we write them and restart SurfaceFlinger (soft reboot, ~23 s). See docs/findings.md.
 * Values are insets in 1/1000 of the screen: 0 = edge, bigger = further in.
 */
class Corners(context: Context) {
    private val prefs = context.getSharedPreferences("corners", Context.MODE_PRIVATE)

    fun status(): String = toJson(read())

    /** Writes the corners and restarts SurfaceFlinger shortly after the reply is sent. */
    @Synchronized
    fun apply(params: Map<String, String>): String {
        val next = KEYS.associateWith { key ->
            val v = params[key]?.toIntOrNull() ?: throw IllegalArgumentException("$key missing")
            require(v in 0..MAX) { "$key must be 0..$MAX" }
            v
        }
        val current = read()
        prefs.edit().putString("undo", KEYS.joinToString(",") { current.getValue(it).toString() }).apply()
        write(next)
        restartLater()
        return """{"applied":true,"seconds":$RESTART_SECONDS}"""
    }

    @Synchronized
    fun undo(): String {
        val saved = prefs.getString("undo", null) ?: throw IllegalStateException("nothing to undo")
        val values = saved.split(",").map { it.toInt() }
        return apply(KEYS.zip(values.map { it.toString() }).toMap())
    }

    private fun read(): Map<String, Int> =
        KEYS.associateWith { Props.get(PREFIX + it).toIntOrNull() ?: 0 }

    private fun write(values: Map<String, Int>) =
        values.forEach { (k, v) -> Props.set(PREFIX + k, v.toString()) }

    private fun restartLater() = thread(isDaemon = true) {
        Thread.sleep(500) // let the HTTP reply go out first; this process dies with zygote
        Props.set("ctl.restart", "surfaceflinger")
    }

    private fun toJson(values: Map<String, Int>) =
        values.entries.joinToString(",", "{", "}") { (k, v) -> "\"$k\":$v" }

    companion object {
        private const val PREFIX = "persist.display.keystone_"
        val KEYS = listOf("ltx", "lty", "rtx", "rty", "lbx", "lby", "rbx", "rby")
        private const val MAX = 450
        private const val RESTART_SECONDS = 25
    }
}

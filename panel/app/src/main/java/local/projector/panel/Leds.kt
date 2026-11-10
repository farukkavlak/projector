package local.projector.panel

import java.io.File

/** The three status LEDs. Their brightness nodes are world-writable. */
object Leds {
    private val NAMES = mapOf("power" to "pwr-r", "status" to "status-g", "blue" to "led3-b")

    fun set(name: String?, on: Boolean): String {
        val node = NAMES[name] ?: throw IllegalArgumentException("bilinmeyen LED: $name")
        File("/sys/class/leds/$node/brightness").writeText(if (on) "1" else "0")
        return status()
    }

    fun isOn(name: String): Boolean =
        runCatching { File("/sys/class/leds/${NAMES.getValue(name)}/brightness").readText().trim() != "0" }
            .getOrDefault(false)

    fun status(): String =
        """{"power":${isOn("power")},"status":${isOn("status")},"blue":${isOn("blue")}}"""
}

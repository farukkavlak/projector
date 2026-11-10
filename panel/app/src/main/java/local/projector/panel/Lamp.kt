package local.projector.panel

import java.io.File
import kotlin.concurrent.thread

/**
 * Lamp on/off. Same order as scripts/lamp.sh; see docs/findings.md.
 * Lamp off stops the fan, and a fan guard then shuts the device down,
 * so the guard is turned off first and re-armed after the fan spins up.
 * Works because bl_power is world-writable and SELinux is permissive.
 */
object Lamp {
    private val BL_POWER = File("/sys/devices/platform/soc@2900000/5240000.ge2d/backlight/tv/bl_power")
    private const val OFF = "5"
    private const val ON = "6"
    private const val FAN_SPINUP_MS = 12_000L

    @Volatile private var busy = false

    fun status(): String = when (BL_POWER.readText().trim()) {
        OFF -> "off"
        "0", ON -> "on"
        else -> "unknown"
    }

    @Synchronized
    fun off(): String {
        check(!busy) { "lamba meşgul" }
        Props.set("sys.shutdown_backlight", "1")
        Props.set("sys.start_fan_fun", "0")
        // Never cut the lamp unless the fan guard is surely off.
        check(Props.get("sys.shutdown_backlight") == "1" && Props.get("sys.start_fan_fun") == "0") {
            "fan koruması hâlâ açık, lambaya dokunulmadı"
        }
        BL_POWER.writeText(OFF)
        return "off"
    }

    @Synchronized
    fun on(): String {
        check(!busy) { "lamba meşgul" }
        BL_POWER.writeText(ON)
        busy = true
        thread(isDaemon = true) {
            try {
                Thread.sleep(FAN_SPINUP_MS)
                Props.set("sys.start_fan_fun", "1")
                Props.set("sys.shutdown_backlight", "0")
            } finally {
                busy = false
            }
        }
        return "on"
    }
}

package local.projector.panel

import java.io.File

/** Manual focus nudge. The motor node is world-writable, so a normal app can move it. */
object Focus {
    private val PATH = File("/sys/devices/platform/motor_ctr/motor_ctrl")

    @Synchronized
    fun nudge(dir: String?): String {
        val (first, then) = when (dir) {
            "up" -> 257 to 386
            "down" -> 513 to 642
            else -> throw IllegalArgumentException("yön up veya down olmalı")
        }
        PATH.writeText(first.toString())
        Thread.sleep(70)
        PATH.writeText(then.toString())
        Thread.sleep(200)
        return status()
    }

    fun status(): String {
        val q = read()
        return """{"raw":"${q.raw}","position":${q.position},"upEnd":${q.upEnd},"downEnd":${q.downEnd}}"""
    }

    fun position(): Int = read().position

    /** Raw value looks like `00.435.41`; the first two chars are end flags, the middle is position. */
    private fun read(): Quad {
        val raw = runCatching { PATH.readText().trim() }.getOrDefault("")
        val position = raw.split(".").getOrNull(1)?.toIntOrNull() ?: 0
        return Quad(raw, position, raw.getOrNull(0) == '1', raw.getOrNull(1) == '1')
    }

    private data class Quad(
        val raw: String,
        val position: Int,
        val upEnd: Boolean,
        val downEnd: Boolean,
    )
}

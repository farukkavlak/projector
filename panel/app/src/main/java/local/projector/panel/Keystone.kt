package local.projector.panel

import android.content.Context
import android.content.Intent

/**
 * Live keystone without reboot. See docs/findings.md.
 *
 * `android.intent.projective_mode` is not protected. system_server then runs
 * `recognizesKeystone(angle)`: it takes the insets from `persist.sys.zoom.value`, adds a tilt
 * correction for `angle`, and sends the corners to SurfaceFlinger.
 * The angle it uses is cached; with `tpryauto=0` the same broadcast loads the cache from
 * `persist.sys.keystoneAngle`. So: load our angle first, then apply.
 *
 * tilt < 0 narrows the bottom edge, tilt > 0 the top. The system skips |tilt| >= 45.
 * scale is the image size in percent (50..100); the system wants an inset in 1/1000 per side.
 */
class Keystone(private val context: Context) {
    private val prefs = context.getSharedPreferences("keystone", Context.MODE_PRIVATE)

    fun status(): String =
        """{"tilt":${prefs.getInt("tilt", DEFAULT_TILT)},"scale":${prefs.getInt("scale", DEFAULT_SCALE)}}"""

    @Synchronized
    fun apply(tilt: Int, scale: Int): String {
        require(tilt in -MAX_TILT..MAX_TILT) { "tilt must be -$MAX_TILT..$MAX_TILT" }
        require(scale in MIN_SCALE..100) { "scale must be $MIN_SCALE..100" }
        val inset = (100 - scale) * 5 // 100% -> 0, 50% -> 250

        Props.set("persist.sys.tpryauto", "0")
        Props.set("persist.sys.keystoneAngle", tilt.toString())
        broadcast() // only caches the angle while tpryauto=0
        Thread.sleep(BROADCAST_GAP_MS)

        Props.set("persist.sys.zoom.value", List(8) { inset }.joinToString(","))
        Props.set("persist.sys.tpryauto", "1")
        broadcast() // applies

        prefs.edit().putInt("tilt", tilt).putInt("scale", scale).apply()
        return status()
    }

    private fun broadcast() = context.sendBroadcast(Intent(ACTION))

    companion object {
        private const val ACTION = "android.intent.projective_mode"
        private const val BROADCAST_GAP_MS = 400L
        const val MAX_TILT = 44
        const val MIN_SCALE = 50
        // Close to the factory setup before we touched it (ceiling, -47 deg sensor).
        private const val DEFAULT_TILT = -30
        private const val DEFAULT_SCALE = 67
    }
}

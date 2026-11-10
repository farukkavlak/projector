package local.projector.panel

import android.content.Context
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import kotlin.concurrent.thread

/**
 * Free 4-corner keystone.
 *
 * The props are always written, so the corners survive a reboot. To show them now we call
 * SurfaceFlinger transact 1050 directly (the same call the OEM settings app makes). That needs
 * `android.permission.HARDWARE_TEST`, which this app only has when signed with the platform key.
 * Without it we restart SurfaceFlinger (~23 s soft reboot) as a fallback. See docs/findings.md.
 *
 * Values are insets in 1/1000 of the screen: 0 = edge, bigger = further in.
 */
class Corners(private val context: Context) {
    private val prefs = context.getSharedPreferences("corners", Context.MODE_PRIVATE)

    fun status(): String = toJson(read())

    /** Writes the corners and applies them live; restarts SurfaceFlinger only if needed. */
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
        val live = applyLive(next)
        if (!live) restartLater()
        return if (live) {
            """{"applied":true,"live":true}"""
        } else {
            """{"applied":true,"live":false,"seconds":$RESTART_SECONDS}"""
        }
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

    /**
     * Sends the corners to SurfaceFlinger: token + 8 floats, order lb, lt, rt, rb (x, y), /1000.
     * `IBinder.transact` throws if the app lacks HARDWARE_TEST, so a failed call is safe to retry
     * with the restart fallback.
     */
    private fun applyLive(values: Map<String, Int>): Boolean {
        val sf = surfaceFlinger ?: return false
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken("android.ui.ISurfaceComposer")
            for (key in TRANSMIT_ORDER) data.writeFloat(values.getValue(key) / 1000f)
            sf.transact(TRANSACT_KEYSTONE, data, reply, 0)
            if (reply.dataAvail() >= 4) reply.readException() // throws when SurfaceFlinger says no
            true
        } catch (e: Throwable) {
            Log.w(TAG, "live keystone not allowed, falling back to a restart: ${e.message}")
            false
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun restartLater() = thread(isDaemon = true) {
        Thread.sleep(500) // let the HTTP reply go out first; this process dies with zygote
        Props.set("ctl.restart", "surfaceflinger")
    }

    private fun toJson(values: Map<String, Int>) =
        values.entries.joinToString(",", "{", "}") { (k, v) -> "\"$k\":$v" }

    private val surfaceFlinger: IBinder? by lazy {
        try {
            val sm = Class.forName("android.os.ServiceManager")
            sm.getMethod("getService", String::class.java).invoke(null, "SurfaceFlinger") as? IBinder
        } catch (e: Throwable) {
            Log.w(TAG, "SurfaceFlinger service not reachable: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "Panel"
        private const val PREFIX = "persist.display.keystone_"
        val KEYS = listOf("ltx", "lty", "rtx", "rty", "lbx", "lby", "rbx", "rby")
        private val TRANSMIT_ORDER = listOf("lbx", "lby", "ltx", "lty", "rtx", "rty", "rbx", "rby")
        private const val TRANSACT_KEYSTONE = 1050
        private const val MAX = 450
        private const val RESTART_SECONDS = 25
    }
}

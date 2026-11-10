package local.projector.panel

import android.util.Log
import java.lang.reflect.InvocationTargetException

/**
 * Display picture settings (brightness, colour temperature) through the vendor HIDL service
 * `vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer`, the same one the OEM settings app uses.
 * The service is reached with reflection because the HIDL Java classes are hidden.
 *
 * Type 1 = brightness (0..100), type 12 = colour temperature (0..2).
 */
object Pq {
    private const val IFACE = "vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer"
    private const val GET = 98
    private const val SET = 97

    fun brightness(): Int? = get(1)
    fun colorTemperature(): Int? = get(12)
    fun setBrightness(value: Int): Int? = set(1, value)
    fun setColorTemperature(value: Int): Int? = set(12, value)

    private fun get(type: Int): Int? = call(GET) { parcel, cls ->
        cls.getMethod("writeInt32", Int::class.javaPrimitiveType).invoke(parcel, type)
    }

    private fun set(type: Int, value: Int): Int? = call(SET) { parcel, cls ->
        val write = cls.getMethod("writeInt32", Int::class.javaPrimitiveType)
        write.invoke(parcel, type)
        write.invoke(parcel, value)
    }

    private fun call(code: Int, write: (Any, Class<*>) -> Unit): Int? = try {
        val binder = Class.forName("android.os.HwBinder")
            .getMethod(
                "getService", String::class.java, String::class.java, Boolean::class.javaPrimitiveType
            )
            .invoke(null, IFACE, "default", true) ?: return null
        val cls = Class.forName("android.os.HwParcel")
        val data = cls.getConstructor().newInstance()
        val reply = cls.getConstructor().newInstance()
        try {
            cls.getMethod("writeInterfaceToken", String::class.java).invoke(data, IFACE)
            write(data, cls)
            Class.forName("android.os.IHwBinder").getMethod(
                "transact", Int::class.javaPrimitiveType, cls, cls, Int::class.javaPrimitiveType
            ).invoke(binder, code, data, reply, 0)
            cls.getMethod("verifySuccess").invoke(reply)
            cls.getMethod("readInt32").invoke(reply) as? Int
        } finally {
            runCatching { cls.getMethod("release").invoke(data) }
            runCatching { cls.getMethod("release").invoke(reply) }
        }
    } catch (e: Throwable) {
        val cause = (e as? InvocationTargetException)?.targetException ?: e
        Log.w("Panel", "PQ call $code failed: ${cause.message}")
        null
    }
}

package local.projector.panel

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Screenshot for the panel. READ_FRAME_BUFFER is signature, granted by the platform key.
 *
 * `screencap` refuses to run from the app process (exit 1, no message), so use the hidden
 * SurfaceControl.screenshot call in-process and fall back to the binary.
 */
object Preview {
    fun png(): ByteArray? = surface() ?: shell()

    private fun surface(): ByteArray? = try {
        val sc = Class.forName("android.view.SurfaceControl")
        val crop = Rect(0, 0, 1280, 720)
        val bitmap = callScreenshot(sc, crop) ?: return null
        val soft = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        ByteArrayOutputStream().use { out ->
            soft.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray().takeIf { it.isNotEmpty() }
        }
    } catch (e: Throwable) {
        Log.w("Panel", "SurfaceControl screenshot failed: ${e.message}")
        null
    }

    private fun callScreenshot(sc: Class<*>, crop: Rect): Bitmap? {
        // Android 11 has both 4-arg (display id) and 5-arg (identity transform + display id).
        val five = runCatching {
            sc.getMethod(
                "screenshot", Rect::class.java, Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType
            ).invoke(null, crop, 1280, 720, false, 0) as? Bitmap
        }.getOrNull()
        if (five != null) return five
        return runCatching {
            sc.getMethod(
                "screenshot", Rect::class.java, Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            ).invoke(null, crop, 1280, 720, 0) as? Bitmap
        }.getOrNull()
    }

    private fun shell(): ByteArray? = try {
        val process = ProcessBuilder("/system/bin/screencap", "-p").start()
        val bytes = process.inputStream.use { it.readBytes() }
        val error = process.errorStream.use { it.readBytes() }
        val code = process.waitFor()
        if (code == 0 && bytes.isNotEmpty()) {
            bytes
        } else {
            Log.w("Panel", "screencap failed ($code): ${error.decodeToString().trim()}")
            null
        }
    } catch (e: Exception) {
        Log.w("Panel", "screencap error: ${e.message}")
        null
    }
}

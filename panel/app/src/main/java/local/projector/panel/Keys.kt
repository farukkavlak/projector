package local.projector.panel

import android.app.Instrumentation
import android.view.KeyEvent

/** D-pad and navigation keys, injected with INJECT_EVENTS (signature, ours via platform signing). */
object Keys {
    private val CODES = mapOf(
        "up" to KeyEvent.KEYCODE_DPAD_UP,
        "down" to KeyEvent.KEYCODE_DPAD_DOWN,
        "left" to KeyEvent.KEYCODE_DPAD_LEFT,
        "right" to KeyEvent.KEYCODE_DPAD_RIGHT,
        "ok" to KeyEvent.KEYCODE_DPAD_CENTER,
        "back" to KeyEvent.KEYCODE_BACK,
        "home" to KeyEvent.KEYCODE_HOME,
        "menu" to KeyEvent.KEYCODE_MENU,
    )

    fun press(name: String?): String {
        val code = CODES[name] ?: throw IllegalArgumentException("bilinmeyen tuş: $name")
        Instrumentation().sendKeyDownUpSync(code)
        return "ok"
    }
}

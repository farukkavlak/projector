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
        "enter" to KeyEvent.KEYCODE_ENTER,
        "del" to KeyEvent.KEYCODE_DEL,
    )

    fun press(name: String?): String {
        val code = CODES[name] ?: throw IllegalArgumentException("unknown key: $name")
        Instrumentation().sendKeyDownUpSync(code)
        return "ok"
    }

    /** Types into the field that has focus on the projector. */
    fun type(text: String): String {
        require(text.length <= MAX_TEXT) { "text is limited to $MAX_TEXT characters" }
        if (text.isNotEmpty()) Instrumentation().sendStringSync(text)
        return "ok"
    }

    /** Clears the focused field: move the cursor to the end, then backspace. */
    fun clear(): String {
        val target = Instrumentation()
        target.sendKeyDownUpSync(KeyEvent.KEYCODE_MOVE_END)
        repeat(CLEAR_STEPS) { target.sendKeyDownUpSync(KeyEvent.KEYCODE_DEL) }
        return "ok"
    }

    private const val MAX_TEXT = 200
    private const val CLEAR_STEPS = 60
}

package local.projector.panel

import android.app.Instrumentation
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent

/**
 * Touch injection for the screen view. INJECT_EVENTS is a signature permission and this app
 * gets it from the platform key. Many TV apps ignore touch, so the UI also has a D-pad mode.
 */
object Touch {
    private const val WIDTH = 1280
    private const val HEIGHT = 720

    @Synchronized
    fun tap(x: Int, y: Int): String {
        val px = x.coerceIn(0, WIDTH - 1).toFloat()
        val py = y.coerceIn(0, HEIGHT - 1).toFloat()
        val now = SystemClock.uptimeMillis()
        val target = Instrumentation()
        send(target, event(now, now, MotionEvent.ACTION_DOWN, px, py))
        send(target, event(now, now + 60, MotionEvent.ACTION_UP, px, py))
        return "ok"
    }

    @Synchronized
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, ms: Int = 300): String {
        val ax = x1.coerceIn(0, WIDTH - 1).toFloat()
        val ay = y1.coerceIn(0, HEIGHT - 1).toFloat()
        val bx = x2.coerceIn(0, WIDTH - 1).toFloat()
        val by = y2.coerceIn(0, HEIGHT - 1).toFloat()
        val start = SystemClock.uptimeMillis()
        val duration = ms.coerceIn(50, 2000)
        val target = Instrumentation()
        send(target, event(start, start, MotionEvent.ACTION_DOWN, ax, ay))
        for (step in 1..10) {
            val f = step / 10f
            send(
                target,
                event(
                    start,
                    start + (duration * f).toLong(),
                    MotionEvent.ACTION_MOVE,
                    ax + (bx - ax) * f,
                    ay + (by - ay) * f,
                )
            )
        }
        send(target, event(start, start + duration, MotionEvent.ACTION_UP, bx, by))
        return "ok"
    }

    private fun event(down: Long, at: Long, action: Int, x: Float, y: Float): MotionEvent =
        MotionEvent.obtain(down, at, action, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }

    private fun send(target: Instrumentation, event: MotionEvent) {
        try {
            target.sendPointerSync(event)
        } finally {
            event.recycle()
        }
    }
}

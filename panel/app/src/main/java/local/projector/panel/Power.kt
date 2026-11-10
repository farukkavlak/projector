package local.projector.panel

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import kotlin.concurrent.thread

/** Restart the panel service, reboot, or shut down. REBOOT/DEVICE_POWER are signature permissions. */
class Power(private val context: Context) {
    fun run(action: String?): String = when (action) {
        "restart" -> restart()
        "reboot" -> { power().reboot("Panel"); "ok" }
        "shutdown" -> { shutdown(); "ok" }
        else -> throw IllegalArgumentException("unknown power action: $action")
    }

    private fun restart(): String {
        thread(isDaemon = true) {
            Thread.sleep(600) // let the HTTP reply go out
            context.stopService(Intent(context, PanelService::class.java))
            Thread.sleep(600)
            PanelService.start(context)
        }
        return "ok"
    }

    private fun shutdown() {
        val method = PowerManager::class.java.getMethod(
            "shutdown", java.lang.Boolean.TYPE, String::class.java, java.lang.Boolean.TYPE
        )
        method.invoke(power(), false, "Panel", false)
    }

    private fun power(): PowerManager = context.getSystemService(PowerManager::class.java)
}

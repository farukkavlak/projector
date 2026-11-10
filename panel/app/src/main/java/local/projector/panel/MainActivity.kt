package local.projector.panel

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PanelService.start(this)
        val view = TextView(this).apply {
            textSize = 32f
            gravity = Gravity.CENTER
            text = "Panel is running\n\nhttp://${localIp()}:${PanelService.PORT}"
        }
        setContentView(view)
    }

    private fun localIp(): String =
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress ?: "?"
}

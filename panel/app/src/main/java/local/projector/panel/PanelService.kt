package local.projector.panel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class PanelService : Service() {
    private var server: HttpServer? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Panel", NotificationManager.IMPORTANCE_MIN)
        )
        val note = Notification.Builder(this, CHANNEL)
            .setContentTitle("Panel")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(1, note)
        server = HttpServer(PORT, Actions(this), assets).also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PORT = 8080
        private const val CHANNEL = "panel"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, PanelService::class.java))
        }
    }
}

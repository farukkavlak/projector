package local.projector.panel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class PanelService : Service() {
    private val servers = mutableListOf<HttpServer>()

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
        val actions = Actions(this)
        servers += HttpServer(PORT, actions, assets).also { it.start() }
        sslContext()?.let { ssl ->
            servers += HttpServer(HTTPS_PORT, actions, assets, ssl).also { it.start() }
        }
    }

    /** HTTPS for the PWA. The certificate and key live in assets; see scripts/make-cert.sh. */
    private fun sslContext(): SSLContext? = try {
        val keyBytes = assets.open("server.key.der").use { it.readBytes() }
        val cert = assets.open("server.crt").use {
            CertificateFactory.getInstance("X.509").generateCertificate(it)
        }
        val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyBytes))
        val store = KeyStore.getInstance("PKCS12")
        store.load(null, null)
        store.setKeyEntry("server", key, charArrayOf(), arrayOf(cert))
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(store, charArrayOf())
        SSLContext.getInstance("TLS").apply { init(kmf.keyManagers, null, null) }
    } catch (e: Exception) {
        Log.w("Panel", "HTTPS disabled: ${e.message}")
        null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        servers.forEach { it.stop() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PORT = 8080
        const val HTTPS_PORT = 8443
        private const val CHANNEL = "panel"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, PanelService::class.java))
        }
    }
}

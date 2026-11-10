package local.projector.panel

import android.app.ActivityManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale

/** Device health for the web UI: temperatures, RAM, storage, Wi-Fi, lamp and the front app. */
class SystemStatus(private val context: Context) {

    fun status(): String {
        val (ramUsed, ramTotal) = ram()
        val (storageFree, storageTotal) = storage()
        val (rssi, ip) = wifi()
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
        return buildString {
            append("""{"cpu":${number(temperature("cpu"))}""")
            append(""","gpu":${number(temperature("gpu"))}""")
            append(""","angle":${number(Props.get("persist.sys.keystoneAngle").toDoubleOrNull())}""")
            append(""","ramUsed":$ramUsed,"ramTotal":$ramTotal""")
            append(""","storageFree":$storageFree,"storageTotal":$storageTotal""")
            append(""","uptime":${SystemClock.elapsedRealtime() / 1000}""")
            append(""","wifiRssi":$rssi,"ip":${json(ip)}""")
            append(""","lamp":${json(Lamp.status())}""")
            append(""","fanGuard":${Props.get("sys.start_fan_fun") == "1"}""")
            append(""","app":${json(foreground())}""")
            append(""","version":${json(version)}}""")
        }
    }

    private fun temperature(kind: String): Double? {
        val zones = File("/sys/class/thermal")
            .listFiles { f -> f.name.startsWith("thermal_zone") } ?: return null
        val zone = zones.firstOrNull { z ->
            runCatching { File(z, "type").readText().trim().contains(kind) }.getOrDefault(false)
        } ?: return null
        val raw = runCatching { File(zone, "temp").readText().trim().toDouble() }.getOrNull()
        return raw?.div(1000.0)
    }

    private fun ram(): Pair<Long, Long> {
        val text = runCatching { File("/proc/meminfo").readText() }.getOrDefault("")
        fun kb(key: String) =
            Regex("$key:\\s+(\\d+)").find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val total = kb("MemTotal") * 1024
        val available = (kb("MemAvailable").takeIf { it > 0 } ?: kb("MemFree")) * 1024
        return (total - available).coerceAtLeast(0) to total
    }

    private fun storage(): Pair<Long, Long> = runCatching {
        val stat = StatFs(context.filesDir.absolutePath)
        stat.availableBytes to stat.totalBytes
    }.getOrDefault(0L to 0L)

    private fun wifi(): Pair<Int, String?> {
        val rssi = runCatching {
            context.getSystemService(WifiManager::class.java).connectionInfo.rssi
        }.getOrDefault(0)
        return rssi to localIp()
    }

    private fun localIp(): String? =
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress

    /** Top activity, if REAL_GET_TASKS is granted. Null when unknown. */
    @Suppress("DEPRECATION")
    private fun foreground(): String? = try {
        val am = context.getSystemService(ActivityManager::class.java)
        val task = am.getRunningTasks(1).firstOrNull()
        (task?.topActivity ?: task?.baseActivity)?.packageName
    } catch (e: Exception) {
        null
    }

    private fun number(value: Double?): String =
        value?.let { String.format(Locale.US, "%.1f", it) } ?: "null"

    private fun json(s: String?): String =
        "\"" + (s ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""
}

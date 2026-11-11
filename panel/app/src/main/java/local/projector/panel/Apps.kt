package local.projector.panel

import android.content.Context
import android.content.Intent
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Launchable apps, so a service can be opened from the phone. */
class Apps(private val context: Context) {
    fun list(): String {
        val pm = context.packageManager
        val items = pm.getInstalledApplications(0).mapNotNull { info ->
            if (info.packageName == context.packageName) return@mapNotNull null
            pm.getLaunchIntentForPackage(info.packageName) ?: return@mapNotNull null
            JSONObject()
                .put("package", info.packageName)
                .put("label", pm.getApplicationLabel(info).toString())
        }.sortedBy { it.getString("label").lowercase(Locale.ROOT) }
        return """{"apps":${JSONArray(items)}}"""
    }

    fun open(pkg: String?): String {
        val name = pkg?.trim().orEmpty()
        require(name.isNotEmpty()) { "package missing" }
        val intent = context.packageManager.getLaunchIntentForPackage(name)
            ?: throw IllegalArgumentException("no launch intent for $name")
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "ok"
    }
}

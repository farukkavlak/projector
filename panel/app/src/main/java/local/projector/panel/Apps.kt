package local.projector.panel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Launchable apps, so a service can be opened from the phone. */
class Apps(private val context: Context) {
    data class Info(val packageName: String, val label: String)

    fun list(): String {
        val items = launchable().map {
            JSONObject().put("package", it.packageName).put("label", it.label)
        }
        return """{"apps":${JSONArray(items)}}"""
    }

    fun open(pkg: String?): String {
        val name = pkg?.trim().orEmpty()
        require(name.isNotEmpty()) { "package missing" }
        val pm = context.packageManager
        val intent = launchIntent(pm, name)
            ?: throw IllegalArgumentException("no launch intent for $name")
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "ok"
    }

    /** First installed package from [target], then a package name that contains its keyword. */
    fun resolve(target: Links.Target): String? {
        val apps = launchable()
        return target.packages.firstOrNull { pkg -> apps.any { it.packageName == pkg } }
            ?: apps.firstOrNull { it.packageName.contains(target.keyword, ignoreCase = true) }?.packageName
    }

    private fun launchable(): List<Info> {
        val pm = context.packageManager
        return pm.getInstalledApplications(0).mapNotNull { info ->
            if (info.packageName == context.packageName) return@mapNotNull null
            launchIntent(pm, info.packageName) ?: return@mapNotNull null
            Info(info.packageName, pm.getApplicationLabel(info).toString())
        }.sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    /** Standard launcher first, then a leanback-only activity (Kick TV has no LAUNCHER). */
    private fun launchIntent(pm: PackageManager, pkg: String): Intent? {
        pm.getLaunchIntentForPackage(pkg)?.let { return it }
        val query = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setPackage(pkg)
        val activity = pm.queryIntentActivities(query, 0).firstOrNull()?.activityInfo ?: return null
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setClassName(activity.packageName, activity.name)
    }
}

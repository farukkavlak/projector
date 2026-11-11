package local.projector.panel

import java.net.URI
import java.util.Locale

/** Maps a web link to the app that should open it. */
object Links {
    /** Packages are tried in order; [keyword] is a last-resort package match. */
    data class Target(val packages: List<String>, val keyword: String)

    private val YOUTUBE = Target(listOf("org.smarttube.stable"), "smarttube")
    private val KICK = Target(
        listOf("com.kick.mobile", "com.kick.androidtv", "tv.kick", "com.kick.tv"),
        "kick",
    )
    private val PRIME = Target(
        listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"),
        "amazonvideo",
    )
    private val TVPLUS = Target(
        listOf("com.turkcell.ott", "com.turkcell.tvplus", "com.ttech.tvplus"),
        "tvplus",
    )
    private val TWITCH = Target(listOf("tv.twitch.android.app"), "twitch")
    private val NETFLIX = Target(listOf("com.netflix.mediaclient"), "netflix")
    private val DISNEY = Target(listOf("com.disney.disneyplus"), "disney")

    fun target(url: String): Target? = when (val host = host(url)) {
        null -> null
        "youtu.be", "youtube.com" -> YOUTUBE
        "kick.com" -> KICK
        "primevideo.com" -> PRIME
        "tvplus.com.tr" -> TVPLUS
        "twitch.tv" -> TWITCH
        "netflix.com" -> NETFLIX
        "disneyplus.com" -> DISNEY
        else -> when {
            host.endsWith(".youtube.com") -> YOUTUBE
            host.endsWith(".kick.com") -> KICK
            host.endsWith(".primevideo.com") -> PRIME
            host.endsWith(".tvplus.com.tr") -> TVPLUS
            host.endsWith(".twitch.tv") -> TWITCH
            host.endsWith(".netflix.com") -> NETFLIX
            host.endsWith(".disneyplus.com") -> DISNEY
            else -> null
        }
    }

    fun host(url: String): String? = runCatching {
        URI(url).host?.removePrefix("www.")?.lowercase(Locale.ROOT)
    }.getOrNull()
}

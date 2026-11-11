package local.projector.panel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinksTest {
    private fun firstPackage(target: Links.Target?) = target?.packages?.first()

    @Test
    fun youtubeOpensSmartTube() {
        assertEquals("org.smarttube.stable", firstPackage(Links.target("https://www.youtube.com/watch?v=x")))
    }

    @Test
    fun shortYoutubeLinksMatch() {
        assertEquals("org.smarttube.stable", firstPackage(Links.target("https://youtu.be/x")))
    }

    @Test
    fun musicYoutubeMatches() {
        assertEquals("org.smarttube.stable", firstPackage(Links.target("https://music.youtube.com/watch?v=x")))
    }

    @Test
    fun kickMatches() {
        assertEquals("kick", Links.target("https://kick.com/xqc")?.keyword)
    }

    @Test
    fun primeMatches() {
        assertEquals("amazonvideo", Links.target("https://www.primevideo.com/detail/x")?.keyword)
    }

    @Test
    fun tvPlusMatches() {
        assertEquals("tvplus", Links.target("https://tvplus.com.tr/")?.keyword)
    }

    @Test
    fun twitchMatches() {
        assertEquals("twitch", Links.target("https://m.twitch.tv/x")?.keyword)
    }

    @Test
    fun unknownSiteHasNoTarget() {
        assertNull(Links.target("https://example.com/video"))
    }

    @Test
    fun invalidUrlHasNoHost() {
        assertNull(Links.host("not a url"))
    }

    @Test
    fun wwwIsStripped() {
        assertEquals("youtube.com", Links.host("https://www.youtube.com/watch?v=x"))
    }
}

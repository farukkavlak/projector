package local.projector.panel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LimitsTest {
    @Test
    fun emptyFileIsRejected() = assertNotNull(Limits.check(0, 0, Long.MAX_VALUE))

    @Test
    fun oversizedFileIsRejected() = assertNotNull(Limits.check(0, Limits.MAX_FILE + 1, Long.MAX_VALUE))

    @Test
    fun quotaIsEnforced() = assertNotNull(Limits.check(Limits.QUOTA, 1, Long.MAX_VALUE))

    @Test
    fun lowFreeSpaceIsRejected() = assertNotNull(Limits.check(0, 1024, 1024))

    @Test
    fun normalFileIsAccepted() = assertNull(Limits.check(0, 1024, Long.MAX_VALUE))

    @Test
    fun nameStripsPaths() = assertEquals("evil.mp4", Limits.safeName("../../evil.mp4"))

    @Test
    fun nameGetsAnExtension() = assertEquals("clip.mp4", Limits.safeName("clip"))

    @Test
    fun nameKeepsAClaimedExtension() = assertEquals("clip.mkv", Limits.safeName("clip.mkv"))

    @Test
    fun videoMimeIsDetected() = assertEquals("video/mp4", Limits.mimeFor("a.mp4"))

    @Test
    fun unknownMimeIsNull() = assertNull(Limits.mimeFor("a.txt"))
}

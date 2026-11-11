package local.projector.panel

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import java.io.InputStream
import java.util.Locale

/**
 * Local videos, stored as app-owned media in Movies/Panel.
 *
 * MediaStore needs no storage permission for files the app creates itself. Playback
 * hands the content URI to VLC with a temporary read grant. Limits are enforced before
 * and during the copy, so a bad Content-Length cannot fill the disk.
 */
class Media(private val context: Context) {
    private val resolver = context.contentResolver
    private val collection: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    fun status(): String {
        val files = query()
        val used = files.sumOf { it.size }
        val body = files.joinToString(",") { it.json() }
        return """{"files":[$body],"used":$used,"quota":${Limits.QUOTA},"free":${freeBytes()}}"""
    }

    @Synchronized
    fun upload(name: String?, mime: String?, input: InputStream, length: Long): String {
        val fileName = Limits.safeName(name)
        val type = (mime?.takeIf { it.startsWith("video/") } ?: Limits.mimeFor(fileName))
            ?: throw IllegalArgumentException("only video files are accepted")
        Limits.check(query().sumOf { it.size }, length, freeBytes())?.let {
            throw IllegalArgumentException(it)
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, type)
            put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("could not create the media entry")
        var written = 0L
        try {
            val output = resolver.openOutputStream(uri, "w")
                ?: throw IllegalStateException("could not open the media file")
            output.use {
                val buffer = ByteArray(BUFFER)
                while (written < length) {
                    val want = minOf(buffer.size.toLong(), length - written).toInt()
                    val read = input.read(buffer, 0, want)
                    if (read < 0) break
                    written += read
                    check(written <= Limits.MAX_FILE) { "file is larger than ${Limits.MAX_FILE} bytes" }
                    it.write(buffer, 0, read)
                }
                it.flush()
            }
            check(written == length) { "upload incomplete" }
            val finalized = resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
            check(finalized > 0) { "could not finalize the media entry" }
            return """{"id":${ContentUris.parseId(uri)},"name":${json(fileName)},"size":$written}"""
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    fun delete(id: Long): String {
        val uri = ContentUris.withAppendedId(collection, id)
        require(resolver.delete(uri, null, null) > 0) { "file not found" }
        return status()
    }

    fun play(id: Long): String {
        val uri = ContentUris.withAppendedId(collection, id)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, resolver.getType(uri) ?: "video/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (installed(VLC)) intent.setPackage(VLC)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            throw IllegalStateException("no video player found; install VLC")
        }
        return "ok"
    }

    private fun query(): List<Item> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? OR " +
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val out = mutableListOf<Item>()
        resolver.query(
            collection,
            projection,
            selection,
            arrayOf(FOLDER, "$FOLDER/%"),
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val size = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val added = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (c.moveToNext()) {
                out += Item(
                    id = c.getLong(id),
                    name = c.getString(name) ?: "",
                    size = c.getLong(size),
                    added = c.getLong(added),
                    mime = c.getString(mime) ?: "video/*",
                )
            }
        }
        return out
    }

    private fun freeBytes(): Long = runCatching {
        StatFs(Environment.getExternalStorageDirectory().absolutePath).availableBytes
    }.getOrDefault(0L)

    private fun installed(pkg: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(pkg) != null

    private data class Item(
        val id: Long,
        val name: String,
        val size: Long,
        val added: Long,
        val mime: String,
    ) {
        fun json(): String =
            """{"id":$id,"name":${json(name)},"size":$size,"added":$added,"mime":${json(mime)}}"""
    }

    companion object {
        private const val VLC = "org.videolan.vlc"
        private val FOLDER = "${Environment.DIRECTORY_MOVIES}/Panel"
        private const val BUFFER = 64 * 1024
    }
}

private fun json(s: String?) =
    "\"" + (s ?: "").replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""

/** Upload limits and name handling. Kept apart from Media so it can be unit tested. */
object Limits {
    const val MAX_FILE = 500L * 1024 * 1024
    const val QUOTA = 2L * 1024 * 1024 * 1024
    private const val MIN_FREE = 50L * 1024 * 1024

    /** Returns why [size] cannot be stored, or null when it fits. */
    fun check(used: Long, size: Long, free: Long): String? = when {
        size <= 0 -> "file is empty"
        size > MAX_FILE -> "file is larger than 500 MB"
        used + size > QUOTA -> "storage quota is full"
        free < size + MIN_FREE -> "not enough free space"
        else -> null
    }

    /** Strips paths and unsafe characters, keeps a video extension. */
    fun safeName(raw: String?): String {
        val base = raw.orEmpty().substringAfterLast('/').substringAfterLast('\\').trim()
        val clean = base.map { c -> if (c.isLetterOrDigit() || c in "._- ") c else '_' }.joinToString("")
        val name = clean.take(120).trim().trim('.').ifEmpty { "video" }
        return if (name.contains('.')) name else "$name.mp4"
    }

    fun mimeFor(name: String): String? = when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "mp4", "m4v" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "ts" -> "video/mp2t"
        "m3u8" -> "application/vnd.apple.mpegurl"
        else -> null
    }
}

package local.projector.panel

import android.content.res.AssetManager
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import javax.net.ssl.SSLContext
import kotlin.concurrent.thread

/** Tiny HTTP/1.0 server. GET serves pages and actions; POST /api/upload streams a video. */
class HttpServer(
    private val port: Int,
    private val actions: Actions,
    private val assets: AssetManager,
    private val ssl: SSLContext? = null,
) {
    private var socket: ServerSocket? = null

    private val READ_TIMEOUT_MS = 15000
    private val MAX_LINE = 8192

    fun start() {
        val s = (ssl?.serverSocketFactory?.createServerSocket(port) ?: ServerSocket(port))
            .also { socket = it }
        thread(name = if (ssl == null) "http" else "https", isDaemon = true) {
            while (!s.isClosed) {
                val client = runCatching { s.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { handle(client) }
            }
        }
    }

    fun stop() {
        socket?.close()
    }

    private fun handle(client: Socket) {
        try {
            client.use { socket ->
                socket.soTimeout = READ_TIMEOUT_MS
                val input = socket.getInputStream()
                val request = readLine(input) ?: return
                val parts = request.split(" ")
                val method = parts.getOrNull(0)?.uppercase() ?: return
                val target = parts.getOrNull(1) ?: "/"
                val headers = readHeaders(input)
                val (route, query) = target.split("?", limit = 2).let { p -> p[0] to p.getOrElse(1) { "" } }
                val out = socket.getOutputStream()
                try {
                    when {
                        method == "POST" && route == "/api/upload" ->
                            upload(out, headers, input, query)
                        method != "GET" ->
                            reply(out, 405, "text/plain", "method not allowed".toByteArray())
                        route.startsWith("/api/") -> {
                            val body = actions.run(route.removePrefix("/api/"), parse(query))
                            reply(out, 200, "application/json", body.toByteArray())
                        }
                        route == "/preview.png" -> {
                            val png = actions.preview()
                            if (png == null) reply(out, 503, "text/plain", "screenshot failed".toByteArray())
                            else reply(out, 200, "image/png", png)
                        }
                        route == "/preview.jpg" -> {
                            val jpg = actions.previewJpeg()
                            if (jpg == null) reply(out, 503, "text/plain", "screenshot failed".toByteArray())
                            else reply(out, 200, "image/jpeg", jpg)
                        }
                        else -> serveAsset(out, route)
                    }
                } catch (e: Exception) {
                    Log.w("Panel", "request failed: $target", e)
                    reply(out, 400, "text/plain", (e.message ?: "error").toByteArray())
                }
            }
        } catch (e: Exception) {
            // Bad TLS handshake, port scan, client gone: close quietly, keep serving.
            Log.w("Panel", "connection closed: ${e.message}")
        }
    }

    /** POST /api/upload?name=<file> streams the raw body into local media. */
    private fun upload(
        out: java.io.OutputStream,
        headers: Map<String, String>,
        input: InputStream,
        query: String,
    ) {
        val length = headers["content-length"]?.toLongOrNull()
        if (headers["expect"]?.contains("100-continue", ignoreCase = true) == true) {
            out.write("HTTP/1.1 100 Continue\r\n\r\n".toByteArray())
            out.flush()
        }
        if (length == null || length <= 0) {
            reply(out, 411, "text/plain", "Content-Length required".toByteArray())
            return
        }
        if (length > Limits.MAX_FILE) {
            reply(out, 413, "text/plain", "file is larger than 500 MB".toByteArray())
            return
        }
        val body = actions.upload(parse(query)["name"], headers["content-type"], input, length)
        reply(out, 200, "application/json", body.toByteArray())
    }

    private fun readHeaders(input: InputStream): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon > 0) {
                headers[line.substring(0, colon).trim().lowercase()] =
                    line.substring(colon + 1).trim()
            }
        }
        return headers
    }

    private fun readLine(input: InputStream): String? {
        val line = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0) return if (line.size() == 0) null else line.toByteArray().decodeToString()
            if (b == '\n'.code) break
            if (b != '\r'.code) line.write(b)
            if (line.size() > MAX_LINE) throw IllegalArgumentException("line too long")
        }
        return line.toByteArray().decodeToString()
    }

    /** Serves index.html for the app and /share, and the other files straight from assets. */
    private fun serveAsset(out: java.io.OutputStream, route: String) {
        val path = when (route) {
            "/", "/share" -> "index.html"
            else -> route.removePrefix("/")
        }
        if (path.isEmpty() || path.contains("..") || path == "server.key.der") {
            reply(out, 404, "text/plain", "not found".toByteArray())
            return
        }
        val bytes = runCatching { assets.open(path).use { it.readBytes() } }.getOrNull()
        if (bytes == null) {
            reply(out, 404, "text/plain", "not found".toByteArray())
            return
        }
        reply(out, 200, contentType(path), bytes)
    }

    private fun contentType(path: String): String = when (path.substringAfterLast('.', "")) {
        "html" -> "text/html; charset=utf-8"
        "js" -> "application/javascript; charset=utf-8"
        "webmanifest" -> "application/manifest+json"
        "json" -> "application/json"
        "png" -> "image/png"
        "svg" -> "image/svg+xml"
        "css" -> "text/css"
        "crt" -> "application/x-x509-ca-cert"
        else -> "application/octet-stream"
    }

    private fun parse(query: String): Map<String, String> =
        query.split("&").filter { it.contains("=") }.associate {
            val (k, v) = it.split("=", limit = 2)
            URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
        }

    private fun reply(out: java.io.OutputStream, code: Int, type: String, body: ByteArray) {
        val head = "HTTP/1.0 $code\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\n" +
            "Cache-Control: no-store\r\nConnection: close\r\n\r\n"
        out.write(head.toByteArray())
        out.write(body)
        out.flush()
    }
}

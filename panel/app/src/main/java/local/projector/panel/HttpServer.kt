package local.projector.panel

import android.content.res.AssetManager
import android.util.Log
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlin.concurrent.thread

/** Tiny HTTP/1.0 server. GET / serves the page, GET /api/<action>?k=v runs an action. */
class HttpServer(
    private val port: Int,
    private val actions: Actions,
    private val assets: AssetManager,
) {
    private var socket: ServerSocket? = null

    fun start() {
        val s = ServerSocket(port).also { socket = it }
        thread(name = "http", isDaemon = true) {
            while (!s.isClosed) {
                val client = runCatching { s.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { handle(client) }
            }
        }
    }

    fun stop() {
        socket?.close()
    }

    private fun handle(client: Socket) = client.use {
        val line = it.getInputStream().bufferedReader().readLine() ?: return
        val target = line.split(" ").getOrNull(1) ?: "/"
        val (route, query) = target.split("?", limit = 2).let { p -> p[0] to p.getOrElse(1) { "" } }
        val out = it.getOutputStream()
        try {
            when {
                route.startsWith("/api/") -> {
                    val body = actions.run(route.removePrefix("/api/"), parse(query))
                    reply(out, 200, "application/json", body.toByteArray())
                }
                else -> serveAsset(out, route)
            }
        } catch (e: Exception) {
            Log.w("Panel", "request failed: $target", e)
            reply(out, 400, "text/plain", (e.message ?: "error").toByteArray())
        }
    }

    /** Serves index.html for the app and /share, and the other files straight from assets. */
    private fun serveAsset(out: java.io.OutputStream, route: String) {
        val path = when (route) {
            "/", "/share" -> "index.html"
            else -> route.removePrefix("/")
        }
        if (path.isEmpty() || path.contains("..")) {
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

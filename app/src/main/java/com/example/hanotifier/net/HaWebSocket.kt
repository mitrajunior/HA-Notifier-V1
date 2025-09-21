package com.example.hanotifier.net

import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.*
import okio.ByteString
import org.json.JSONObject

class HaWebSocket(
  private val url: String,
  private val token: String?,
  private val onEvent: (String) -> Unit,
  private val onDisconnect: (Throwable?) -> Unit
) : WebSocketListener() {
  private var ws: WebSocket? = null
  private var msgId = 1
  private val closed = AtomicBoolean(false)

  fun connect(client: OkHttpClient = OkHttpClient()): WebSocket {
    val req = Request.Builder().url(url).build()
    val socket = client.newWebSocket(req, this)
    ws = socket
    return socket
  }

  fun close() {
    if (closed.compareAndSet(false, true)) {
      ws?.close(1000, "bye")
    } else {
      ws?.cancel()
    }
  }

  override fun onOpen(webSocket: WebSocket, response: Response) {
    if (token == null) {
      if (closed.compareAndSet(false, true)) {
        onDisconnect(AuthException("Token em falta"))
      }
      webSocket.close(1008, "Token em falta")
    }
  }

  override fun onMessage(webSocket: WebSocket, text: String) {
    val type = try {
      JSONObject(text).optString("type")
    } catch (_: Throwable) {
      null
    }
    when (type) {
      "auth_required" -> {
        if (token == null) {
          if (closed.compareAndSet(false, true)) {
            onDisconnect(AuthException("Token em falta"))
          }
          webSocket.close(1008, "Token em falta")
        } else {
          val auth = JSONObject()
            .put("type", "auth")
            .put("access_token", token)
          webSocket.send(auth.toString())
        }
      }
      "auth_ok" -> {
        val payload = JSONObject()
          .put("id", msgId++)
          .put("type", "subscribe_events")
          .put("event_type", "app_notify")
        webSocket.send(payload.toString())
      }
      "auth_invalid" -> {
        if (closed.compareAndSet(false, true)) {
          onDisconnect(AuthException("Token inválido"))
        }
        webSocket.close(4001, "Token inválido")
      }
      "event" -> onEvent(text)
      else -> {
        if (text.contains("event")) {
          onEvent(text)
        }
      }
    }
  }

  override fun onMessage(webSocket: WebSocket, bytes: ByteString) { }

  override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
    if (closed.compareAndSet(false, true)) {
      onDisconnect(null)
    }
  }

  override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
    if (closed.compareAndSet(false, true)) {
      onDisconnect(t)
    }
  }

  class AuthException(message: String) : Exception(message)
}

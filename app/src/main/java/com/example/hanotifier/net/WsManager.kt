package com.example.hanotifier.net

import android.content.Context
import android.util.Log
import com.example.hanotifier.data.Payload
import com.example.hanotifier.notify.NotificationHelper
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import org.json.JSONObject

enum class WsState { CONNECTING, CONNECTED, DISCONNECTED }

object WsManager {
  private const val TAG = "WsManager"
  @Volatile private var running = false
  private var socket: HaWebSocket? = null
  private var scope: CoroutineScope? = null
  private var onState: ((WsState) -> Unit)? = null
  private var currentUrl: String? = null
  private var currentToken: String? = null

  private val _state = MutableStateFlow(WsState.DISCONNECTED)
  val state: StateFlow<WsState> get() = _state

  fun buildWsUrl(base: String?): String? {
    if (base.isNullOrBlank()) return null
    val trimmed = base.trimEnd('/')
    return when {
      trimmed.startsWith("https://") -> trimmed.replaceFirst("https://", "wss://") + "/api/websocket"
      trimmed.startsWith("http://") -> trimmed.replaceFirst("http://", "ws://") + "/api/websocket"
      trimmed.startsWith("wss://") || trimmed.startsWith("ws://") -> trimmed
      else -> "wss://$trimmed/api/websocket"
    }
  }

  fun startInService(ctx: Context, wsUrl: String, token: String?, onStateChange: (WsState) -> Unit) {
    onState = onStateChange
    if (running && wsUrl == currentUrl && token == currentToken) {
      return
    }
    if (running) {
      stop()
    }
    running = true
    currentUrl = wsUrl
    currentToken = token
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    scope?.launch {
      val client = OkHttpClient()
      var attempt = 0
      while (running) {
        val disconnectSignal = CompletableDeferred<Unit>()
        var fatalError: Throwable? = null
        try {
          updateState(WsState.CONNECTING)
          socket = HaWebSocket(wsUrl, token, { text -> handleEvent(ctx, text) }) { throwable ->
            if (throwable != null) {
              if (throwable is HaWebSocket.AuthException) {
                fatalError = throwable
                Log.e(TAG, "Autenticação WebSocket falhou: ${throwable.message}")
              } else {
                Log.w(TAG, "Socket desconectado", throwable)
              }
            } else {
              Log.i(TAG, "Socket encerrado")
            }
            if (!disconnectSignal.isCompleted) {
              disconnectSignal.complete(Unit)
            }
          }
          socket?.connect(client)
          updateState(WsState.CONNECTED)
          attempt = 0
          disconnectSignal.await()
        } catch (t: Throwable) {
          if (t is CancellationException) {
            throw t
          }
          Log.w(TAG, "Falha na ligação WS", t)
          if (!disconnectSignal.isCompleted) {
            if (fatalError == null) {
              fatalError = t
            }
            disconnectSignal.complete(Unit)
          }
        } finally {
          socket?.close()
          socket = null
        }
        if (!running) break
        val fatal = fatalError
        if (fatal is HaWebSocket.AuthException) {
          running = false
          updateState(WsState.DISCONNECTED)
          break
        }
        updateState(WsState.DISCONNECTED)
        attempt++
        val backoff = min(30_000L, 2_000L * attempt.toLong())
        delay(backoff)
      }
      updateState(WsState.DISCONNECTED)
    }
  }

  private fun handleEvent(ctx: Context, text: String) {
    try {
      val root = JSONObject(text)
      if (!root.optString("type").equals("event")) return
      val ev = root.getJSONObject("event")
      val data = ev.optJSONObject("data") ?: return
      val payload = Payload(
        title = data.optString("title", "Alerta"),
        body = data.optString("body", ""),
        priority = data.optString("priority", "info"),
        persistent = data.optBoolean("persistent", false),
        popup = data.optBoolean("popup", false),
        requireAck = data.optBoolean("requireAck", false),
        channel = data.optString("channel", null),
        sound = data.optString("sound", null),
        vibration = data.optJSONArray("vibration")?.let { arr -> MutableList(arr.length()) { i -> arr.getLong(i) } },
        actions = null,
        image = data.optString("image", null),
        timeout_sec = data.optInt("timeout_sec", 0),
        collapseKey = data.optString("collapse_key", (data.optString("title", "") + data.optString("body", "")).take(48)),
        group = data.optString("group", null)
      )
      NotificationHelper.show(ctx, payload)
    } catch (t: Throwable) {
      Log.e(TAG, "Erro a processar evento", t)
    }
  }

  fun stop() {
    running = false
    currentUrl = null
    currentToken = null
    scope?.cancel()
    scope = null
    socket?.close()
    socket = null
    updateState(WsState.DISCONNECTED)
  }

  private fun updateState(state: WsState) {
    _state.value = state
    onState?.invoke(state)
  }
}

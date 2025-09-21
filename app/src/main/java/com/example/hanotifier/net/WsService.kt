package com.example.hanotifier.net

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.hanotifier.MainActivity
import com.example.hanotifier.R
import com.example.hanotifier.data.Prefs
import com.example.hanotifier.notify.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class WsService : LifecycleService() {

  companion object {
    const val NOTIF_ID = 42
    const val CH_ID = "ws_foreground"

    fun start(ctx: Context) {
      if (!NotificationHelper.canPostNotifications(ctx)) {
        return
      }
      val intent = Intent(ctx, WsService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ctx.startForegroundService(intent)
      } else {
        ctx.startService(intent)
      }
    }

    fun stop(ctx: Context) {
      val intent = Intent(ctx, WsService::class.java)
      ctx.stopService(intent)
    }
  }

  private var observeJob: Job? = null

  override fun onCreate() {
    super.onCreate()
    ensureChannel()
    startForeground(NOTIF_ID, buildNotification("A iniciar…"))

    val prefs = Prefs(this)
    observeJob = lifecycleScope.launch {
      combine(
        prefs.lanUrl,
        prefs.wanUrl,
        prefs.token,
        prefs.wsEnabled,
        prefs.wsPreferLan
      ) { lan, wan, token, enabled, preferLan ->
        val trimmedToken = token.trim()
        WsConfig(
          enabled = enabled,
          wsUrl = WsManager.buildWsUrl(
            (if (preferLan) lan.takeIf { it.isNotBlank() } else wan.takeIf { it.isNotBlank() })
              ?: lan.takeIf { it.isNotBlank() } ?: wan
          ),
          token = trimmedToken.takeIf { it.isNotBlank() }
        )
      }.collect { config ->
        val (enabled, wsUrl, token) = config
        if (enabled && !wsUrl.isNullOrBlank() && token != null) {
          WsManager.startInService(this@WsService, wsUrl, token) { state ->
            val label = when (state) {
              WsState.CONNECTED -> "Ligado ao Home Assistant"
              WsState.CONNECTING -> "A ligar…"
              WsState.DISCONNECTED -> "Desligado"
            }
            updateNotification(label)
          }
        } else {
          WsManager.stop()
          val message = when {
            !enabled -> "Desativado"
            wsUrl.isNullOrBlank() -> "URL em falta"
            token == null -> "Token em falta"
            else -> "Configuração inválida"
          }
          updateNotification(message)
        }
      }
    }
  }

  private data class WsConfig(
    val enabled: Boolean,
    val wsUrl: String?,
    val token: String?
  )

  override fun onDestroy() {
    observeJob?.cancel()
    WsManager.stop()
    super.onDestroy()
  }

  private fun ensureChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (nm.getNotificationChannel(CH_ID) == null) {
        val channel = NotificationChannel(
          CH_ID,
          "Ligação Home Assistant",
          NotificationManager.IMPORTANCE_MIN
        )
        nm.createNotificationChannel(channel)
      }
    }
  }

  private fun buildNotification(text: String): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or (
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
      )
    )
    return NotificationCompat.Builder(this, CH_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("HA Notifier")
      .setContentText(text)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .build()
  }

  private fun updateNotification(text: String) {
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIF_ID, buildNotification(text))
  }

  override fun onBind(intent: Intent): IBinder? = super.onBind(intent)
}

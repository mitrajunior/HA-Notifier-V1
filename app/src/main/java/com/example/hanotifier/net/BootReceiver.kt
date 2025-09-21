package com.example.hanotifier.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.hanotifier.data.Prefs
import com.example.hanotifier.notify.NotificationHelper
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
      val prefs = Prefs(context)
      runBlocking {
        val enabled = prefs.wsEnabled.firstOrNull() ?: false
        if (enabled && NotificationHelper.canPostNotifications(context)) {
          WsService.start(context)
        }
      }
    }
  }
}

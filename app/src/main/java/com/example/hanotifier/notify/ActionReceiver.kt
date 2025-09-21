package com.example.hanotifier.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.hanotifier.data.Action as PayloadAction

class ActionReceiver : BroadcastReceiver() {

  companion object {
    const val EXTRA_ACTION = "com.example.hanotifier.extra.ACTION"
    const val EXTRA_NOTIFICATION_ID = "com.example.hanotifier.extra.NOTIFICATION_ID"
  }

  override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    val action = intent.getSerializableExtra(EXTRA_ACTION) as? PayloadAction
    if (action == null) {
      pendingResult.finish()
      return
    }
    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
    ActionExecutor.execute(context.applicationContext, action) { result ->
      if (result.success && notificationId != 0) {
        NotificationManagerCompat.from(context).cancel(notificationId)
      }
      pendingResult.finish()
    }
  }
}

// MainActivity.kt
package com.example.hanotifier

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.hanotifier.net.WsService
import com.example.hanotifier.notify.NotificationHelper
import com.example.hanotifier.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

  private var notificationsRequested = false

  private val requestNotifications = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      NotificationHelper.ensureChannels(this)
      WsService.start(this)
    }
    notificationsRequested = true
    if (!granted) {
      NotificationHelper.ensureChannels(this)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ensureForegroundReady()
    setContent { AppTheme { AppNav() } }
  }

  override fun onResume() {
    super.onResume()
    ensureForegroundReady()
  }

  private fun ensureForegroundReady() {
    if (NotificationHelper.canPostNotifications(this)) {
      NotificationHelper.ensureChannels(this)
      WsService.start(this)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsRequested) {
      notificationsRequested = true
      requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}

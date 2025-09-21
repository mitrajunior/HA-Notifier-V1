package com.example.hanotifier.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.hanotifier.data.Prefs
import com.example.hanotifier.notify.NotificationHelper
import com.example.hanotifier.net.WsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(padding: PaddingValues) {
  val ctx = LocalContext.current
  val prefs = remember { Prefs(ctx) }
  val writeScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
  val notificationsAllowed = NotificationHelper.canPostNotifications(ctx)

  val lanFlow by prefs.lanUrl.collectAsState(initial = "")
  val wanFlow by prefs.wanUrl.collectAsState(initial = "")
  val tokenFlow by prefs.token.collectAsState(initial = "")
  val fullScreen by prefs.fullScreen.collectAsState(initial = true)
  val persistent by prefs.persistent.collectAsState(initial = true)
  val wsEnabled by prefs.wsEnabled.collectAsState(initial = false)
  val wsPreferLan by prefs.wsPreferLan.collectAsState(initial = true)

  var lan by remember { mutableStateOf(lanFlow) }
  var wan by remember { mutableStateOf(wanFlow) }
  var token by remember { mutableStateOf(tokenFlow) }
  var showToken by remember { mutableStateOf(false) }

  LaunchedEffect(lanFlow) { lan = lanFlow }
  LaunchedEffect(wanFlow) { wan = wanFlow }
  LaunchedEffect(tokenFlow) { token = tokenFlow }

  val saveJob = remember { mutableStateOf<Job?>(null) }
  fun saveInputs() {
    saveJob.value?.cancel()
    saveJob.value = writeScope.launch {
      prefs.setLanUrl(lan.trim())
      prefs.setWanUrl(wan.trim())
      prefs.setToken(token.trim())
    }
  }

  fun scheduleSave() {
    saveJob.value?.cancel()
    saveJob.value = writeScope.launch {
      delay(500)
      prefs.setLanUrl(lan.trim())
      prefs.setWanUrl(wan.trim())
      prefs.setToken(token.trim())
    }
  }

  Column(
    Modifier
      .padding(padding)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    if (!notificationsAllowed) {
      ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Permitir notificações", style = MaterialTheme.typography.titleMedium)
          Text("A app precisa da permissão de notificações para manter a ligação e mostrar alertas.")
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
              try {
                ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                  putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
              } catch (_: Throwable) {
                Toast.makeText(ctx, "Abre manualmente as definições da app.", Toast.LENGTH_SHORT).show()
              }
            }) { Text("Abrir definições") }
          }
        }
      }
    }

    Text("Ligação ao Home Assistant", style = MaterialTheme.typography.titleMedium)

    OutlinedTextField(
      value = lan,
      onValueChange = { value -> lan = value; scheduleSave() },
      label = { Text("URL LAN (ex: http://ha.local:8123)") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Uri,
        autoCorrectEnabled = false,
        imeAction = ImeAction.Next
      )
    )

    OutlinedTextField(
      value = wan,
      onValueChange = { value -> wan = value; scheduleSave() },
      label = { Text("URL Externa (Nabu Casa/reverse proxy)") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Uri,
        autoCorrectEnabled = false,
        imeAction = ImeAction.Next
      )
    )

    OutlinedTextField(
      value = token,
      onValueChange = { value -> token = value; scheduleSave() },
      label = { Text("Long-Lived Token") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        autoCorrectEnabled = false,
        imeAction = ImeAction.Done
      ),
      trailingIcon = {
        IconButton(onClick = { showToken = !showToken }) {
          Icon(
            imageVector = if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (showToken) "Ocultar" else "Mostrar"
          )
        }
      }
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(onClick = { saveInputs() }) { Text("Guardar agora") }
      OutlinedButton(onClick = {
        val base = (if (wsPreferLan) lan.takeIf { it.isNotBlank() } else wan.takeIf { it.isNotBlank() })
          ?: lan.takeIf { it.isNotBlank() }
          ?: wan.takeIf { it.isNotBlank() }
        val url = WsManager.buildWsUrl(base?.trim())
        val msg = if (!url.isNullOrBlank()) "URL OK: $url" else "URL inválido"
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
      }) { Text("Testar ligação") }
    }

    OutlinedButton(onClick = {
      try {
        val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
      } catch (_: Throwable) {
        Toast.makeText(ctx, "Não foi possível abrir as definições", Toast.LENGTH_SHORT).show()
      }
    }) { Text("Abrir definições de bateria") }

    Divider()
    Text("WebSocket (LAN/Externo)", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
      Switch(checked = wsEnabled, onCheckedChange = { checked ->
        writeScope.launch { prefs.setWsEnabled(checked) }
      })
      Spacer(Modifier.width(8.dp))
      Text("Ativar subscrição WebSocket (app_notify)")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Switch(checked = wsPreferLan, onCheckedChange = { checked ->
        writeScope.launch { prefs.setWsPreferLan(checked) }
      })
      Spacer(Modifier.width(8.dp))
      Text("Preferir URL LAN")
    }

    Divider()
    Text("Comportamento de notificações", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
      Switch(checked = fullScreen, onCheckedChange = { writeScope.launch { prefs.setFullScreen(it) } })
      Spacer(Modifier.width(8.dp))
      Text("Usar popup (full-screen) para crítico")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Switch(checked = persistent, onCheckedChange = { writeScope.launch { prefs.setPersistent(it) } })
      Spacer(Modifier.width(8.dp))
      Text("Tornar crítico persistente")
    }

    Spacer(Modifier.height(16.dp))
    Text("Dica: toca no ícone ⚙️ no topo para voltar aqui sempre que precisares.")
  }
}

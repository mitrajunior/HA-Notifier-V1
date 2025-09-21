package com.example.hanotifier.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.hanotifier.notify.NotificationHelper
import com.example.hanotifier.data.Payload

@Composable
fun HomeScreen(padding: PaddingValues) {
  val ctx = LocalContext.current
  Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Testes rápidos", style = MaterialTheme.typography.titleMedium)

    Button(onClick = {
      NotificationHelper.show(
        ctx = ctx,
        payload = Payload(
          title = "Teste",
          body = "Isto é uma notificação de teste",
          priority = "warning",
          persistent = false,
          popup = false
        )
      )
    }) {
      Text("Notificação simples")
    }

    Button(onClick = {
      NotificationHelper.show(
        ctx = ctx,
        payload = Payload(
          title = "Alerta Crítico",
          body = "Popup em ecrã inteiro",
          priority = "critical",
          persistent = true,
          popup = true,
          requireAck = true
        )
      )
    }) { Text("Alerta popup (full‑screen)") }
  }
}

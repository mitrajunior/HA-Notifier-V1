package com.example.hanotifier.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hanotifier.data.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(padding: PaddingValues) {
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()
  val dao = remember { DbProvider.get(ctx).templates() }

  var itemsList by remember { mutableStateOf<List<Template>>(emptyList()) }
  var name by remember { mutableStateOf("") }
  var priority by remember { mutableStateOf("warning") }
  var persistent by remember { mutableStateOf(false) }
  var popup by remember { mutableStateOf(false) }
  var requireAck by remember { mutableStateOf(false) }

  fun load() { scope.launch { itemsList = dao.all() } }
  LaunchedEffect(Unit) { load() }

  Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Modelos (Templates) de Notificações", style = MaterialTheme.typography.titleMedium)

    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do template") }, modifier = Modifier.fillMaxWidth())

    Row {
      Text("Prioridade: ")
      Spacer(Modifier.width(8.dp))
      listOf("info", "warning", "critical").forEach { opt ->
        FilterChip(
          selected = (priority == opt),
          onClick = { priority = opt },
          label = { Text(opt) }
        )
        Spacer(Modifier.width(8.dp))
      }
    }

    Row { Switch(persistent, { persistent = it }); Spacer(Modifier.width(8.dp)); Text("Persistente") }
    Row { Switch(popup, { popup = it }); Spacer(Modifier.width(8.dp)); Text("Popup (full-screen se crítico)") }
    Row { Switch(requireAck, { requireAck = it }); Spacer(Modifier.width(8.dp)); Text("Exigir reconhecimento") }

    Button(onClick = {
      if (name.isNotBlank()) scope.launch {
        dao.insert(Template(name = name, priority = priority, persistent = persistent, popup = popup, requireAck = requireAck))
        name = ""; load()
      }
    }) { Text("Guardar template") }

    Divider()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(itemsList) { t ->
        Card {
          Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
              Text(t.name, style = MaterialTheme.typography.titleMedium)
              Text("prio=${t.priority} | persistente=${t.persistent} | popup=${t.popup}")
            }
            OutlinedButton(onClick = { scope.launch { dao.delete(t); load() } }) { Text("Apagar") }
          }
        }
      }
    }
  }
}

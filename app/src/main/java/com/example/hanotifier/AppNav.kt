package com.example.hanotifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hanotifier.ui.screens.HomeScreen
import com.example.hanotifier.ui.screens.SettingsScreen
import com.example.hanotifier.ui.screens.TemplatesScreen
import com.example.hanotifier.net.WsManager
import com.example.hanotifier.net.WsState

sealed class Route(val route: String) {
  data object Home: Route("home")
  data object Settings: Route("settings")
  data object Templates: Route("templates")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
  val nav = rememberNavController()
  val backStack by nav.currentBackStackEntryAsState()

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("HA Notifier") },
        actions = {
          val wsState by WsManager.state.collectAsState()
          WsStateIndicator(wsState)
          IconButton(onClick = { nav.navigate(Route.Templates.route) }) {
            Icon(
              painterResource(android.R.drawable.ic_menu_add),
              contentDescription = "Templates"
            )
          }
          IconButton(onClick = { nav.navigate(Route.Settings.route) }) {
            Icon(
              painterResource(android.R.drawable.ic_menu_manage),
              contentDescription = "Definições"
            )
          }
        }
      )
    }
  ) { padding ->
    NavHost(navController = nav, startDestination = Route.Home.route) {
      composable(Route.Home.route) { HomeScreen(padding) }
      composable(Route.Settings.route) { SettingsScreen(padding) }
      composable(Route.Templates.route) { TemplatesScreen(padding) }
    }
  }
}

@Composable
private fun RowScope.WsStateIndicator(state: WsState) {
  val (label, color) = when (state) {
    WsState.CONNECTED -> "Ligado" to MaterialTheme.colorScheme.tertiary
    WsState.CONNECTING -> "A ligar…" to MaterialTheme.colorScheme.secondary
    WsState.DISCONNECTED -> "Desligado" to MaterialTheme.colorScheme.error
  }
  Row(
    modifier = Modifier
      .padding(end = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .background(color, CircleShape)
    )
    Text(label, style = MaterialTheme.typography.labelSmall)
  }
}

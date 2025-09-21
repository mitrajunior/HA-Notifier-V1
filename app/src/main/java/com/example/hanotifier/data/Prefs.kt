package com.example.hanotifier.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "prefs")

class Prefs(private val ctx: Context) {
  private val LAN_URL = stringPreferencesKey("lanUrl")
  private val WAN_URL = stringPreferencesKey("wanUrl")
  private val TOKEN = stringPreferencesKey("token")
  private val FULLSCREEN = booleanPreferencesKey("fullScreen")
  private val PERSISTENT = booleanPreferencesKey("persistent")
  private val WS_ENABLED = booleanPreferencesKey("wsEnabled")
  private val WS_PREFER_LAN = booleanPreferencesKey("wsPreferLan")

  val lanUrl = ctx.dataStore.data.map { it[LAN_URL] ?: "" }
  val wanUrl = ctx.dataStore.data.map { it[WAN_URL] ?: "" }
  val token = ctx.dataStore.data.map { it[TOKEN] ?: "" }
  val fullScreen = ctx.dataStore.data.map { it[FULLSCREEN] ?: true }
  val persistent = ctx.dataStore.data.map { it[PERSISTENT] ?: true }
  val wsEnabled = ctx.dataStore.data.map { it[WS_ENABLED] ?: false }
  val wsPreferLan = ctx.dataStore.data.map { it[WS_PREFER_LAN] ?: true }

  suspend fun setLanUrl(v: String) { ctx.dataStore.edit { it[LAN_URL] = v } }
  suspend fun setWanUrl(v: String) { ctx.dataStore.edit { it[WAN_URL] = v } }
  suspend fun setToken(v: String) { ctx.dataStore.edit { it[TOKEN] = v } }
  suspend fun setFullScreen(v: Boolean) { ctx.dataStore.edit { it[FULLSCREEN] = v } }
  suspend fun setPersistent(v: Boolean) { ctx.dataStore.edit { it[PERSISTENT] = v } }
  suspend fun setWsEnabled(v: Boolean) { ctx.dataStore.edit { it[WS_ENABLED] = v } }
  suspend fun setWsPreferLan(v: Boolean) { ctx.dataStore.edit { it[WS_PREFER_LAN] = v } }
}

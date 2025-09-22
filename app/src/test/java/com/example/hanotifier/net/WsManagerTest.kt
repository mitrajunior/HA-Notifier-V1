package com.example.hanotifier.net

import android.content.Context
import com.example.hanotifier.data.Payload
import com.example.hanotifier.notify.NotificationHelper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class WsManagerTest {

  @Before
  fun setUp() {
    mockkObject(NotificationHelper)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `handleEvent parses YAML payload and triggers notification`() {
    val ctx = mockk<Context>(relaxed = true)
    val dataYaml = """
      title: Teste YAML
      body: Mensagem YAML
      priority: critical
      persistent: true
      popup: true
      actions:
        - title: Abrir Site
          type: url
          url: https://example.com
      vibration:
        - 100
        - 200
    """.trimIndent()
    val event = JSONObject().apply {
      put("type", "event")
      put("event", JSONObject().apply {
        put("event_type", "app_notify")
        put("data", dataYaml)
      })
    }

    val capturedPayload = slot<Payload>()
    every { NotificationHelper.show(ctx, capture(capturedPayload)) } just Runs

    val method = WsManager::class.java.getDeclaredMethod("handleEvent", Context::class.java, String::class.java)
    method.isAccessible = true
    method.invoke(WsManager, ctx, event.toString())

    verify(exactly = 1) { NotificationHelper.show(ctx, any()) }
    val payload = capturedPayload.captured
    assertEquals("Teste YAML", payload.title)
    assertEquals("Mensagem YAML", payload.body)
    assertEquals("critical", payload.priority)
    assertEquals(true, payload.persistent)
    assertEquals(true, payload.popup)
    assertNotNull(payload.vibration)
    assertEquals(listOf(100L, 200L), payload.vibration)
    val actions = payload.actions
    assertNotNull(actions)
    assertEquals(1, actions.size)
    val action = actions.first()
    assertEquals("Abrir Site", action.title)
    assertEquals("url", action.type)
    assertEquals("https://example.com", action.url)
    assertTrue(payload.collapseKey.isNotBlank())
  }
}

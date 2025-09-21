package com.example.hanotifier.data

import java.io.Serializable

data class Action(
  val title: String,
  val type: String? = null, // "ha_service" | "url"
  val service: String? = null,
  val entity_id: String? = null,
  val url: String? = null,
) : Serializable

data class Payload(
  val title: String,
  val body: String,
  val priority: String? = "info", // may be null to inherit
  val persistent: Boolean? = null,
  val popup: Boolean? = null,
  val requireAck: Boolean? = null,
  val channel: String? = null,
  val sound: String? = null,
  val vibration: List<Long>? = null,
  val actions: List<Action>? = null,
  val image: String? = null,
  val timeout_sec: Int? = 0,
  val collapseKey: String = (title + body).take(48),
  val group: String? = null,
  val templateId: Long? = null,
  val templateName: String? = null,
)

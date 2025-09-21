package com.example.hanotifier.net

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class HaRest(baseUrl: String, token: String?) {
  private val client: OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
    .build()
  private val base = baseUrl.trimEnd('/')
  private val auth = token?.let { "Bearer $it" }

  fun callService(domain: String, service: String, jsonBody: String): Response {
    val url = "$base/api/services/$domain/$service"
    val req = Request.Builder().url(url)
      .post(RequestBody.create("application/json".toMediaType(), jsonBody))
      .apply { if (auth != null) header("Authorization", auth) }
      .build()
    return client.newCall(req).execute()
  }
}

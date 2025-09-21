package com.example.hanotifier.notify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.hanotifier.R
import com.example.hanotifier.data.Prefs
import com.example.hanotifier.net.HaRest
import com.example.hanotifier.data.Action as PayloadAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

object ActionExecutor {

  data class Result(val success: Boolean, val message: String? = null)

  private const val TAG = "ActionExecutor"

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun execute(context: Context, action: PayloadAction, onResult: ((Result) -> Unit)? = null) {
    val appCtx = context.applicationContext
    val kind = action.type?.lowercase() ?: when {
      !action.service.isNullOrBlank() -> "ha_service"
      !action.url.isNullOrBlank() -> "url"
      else -> null
    }
    when (kind) {
      "ha_service" -> triggerService(appCtx, action, onResult)
      "url" -> openUrl(appCtx, action.url, onResult)
      else -> postResult(appCtx, onResult, Result(false, appCtx.getString(R.string.action_missing_target)))
    }
  }

  fun openLink(context: Context, url: String, onResult: ((Result) -> Unit)? = null) {
    openUrl(context.applicationContext, url, onResult)
  }

  private fun openUrl(context: Context, rawUrl: String?, onResult: ((Result) -> Unit)?) {
    val normalized = normalizeUrl(rawUrl)
    if (normalized == null) {
      postResult(context, onResult, Result(false, context.getString(R.string.action_link_error)))
      return
    }
    try {
      val intent = Intent(Intent.ACTION_VIEW, normalized).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      postResult(context, onResult, Result(true))
    } catch (t: Throwable) {
      Log.w(TAG, "Failed to open link", t)
      postResult(context, onResult, Result(false, context.getString(R.string.action_link_error)))
    }
  }

  private fun triggerService(context: Context, action: PayloadAction, onResult: ((Result) -> Unit)?) {
    scope.launch {
      val result = callService(context, action)
      postResult(context, onResult, result)
    }
  }

  private suspend fun callService(context: Context, action: PayloadAction): Result {
    val serviceName = action.service?.takeIf { it.isNotBlank() }
      ?: return Result(false, context.getString(R.string.action_missing_service))
    val parts = serviceName.split('.', limit = 2)
    if (parts.size != 2) {
      return Result(false, context.getString(R.string.action_invalid_service))
    }
    val (domain, service) = parts

    val prefs = Prefs(context)
    val baseUrl = withContext(Dispatchers.IO) {
      prefs.lanUrl.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: prefs.wanUrl.firstOrNull()?.takeIf { it.isNotBlank() }
    } ?: return Result(false, context.getString(R.string.action_missing_url))

    val token = withContext(Dispatchers.IO) {
      prefs.token.firstOrNull()?.takeIf { it.isNotBlank() }
    } ?: return Result(false, context.getString(R.string.action_missing_token))

    val body = JSONObject().apply {
      action.entity_id?.takeIf { it.isNotBlank() }?.let { put("entity_id", it) }
    }.toString()

    return try {
      HaRest(baseUrl, token).callService(domain, service, body).use { resp ->
        if (resp.isSuccessful) {
          Result(true, context.getString(R.string.action_sent))
        } else {
          Result(false, context.getString(R.string.action_failed_code, resp.code))
        }
      }
    } catch (t: Throwable) {
      Log.e(TAG, "Failed to call service", t)
      Result(false, context.getString(R.string.action_failed_generic))
    }
  }

  private fun normalizeUrl(raw: String?): Uri? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    val prefixed = if (Uri.parse(trimmed).scheme.isNullOrBlank()) {
      "https://$trimmed"
    } else {
      trimmed
    }
    return runCatching { Uri.parse(prefixed) }.getOrNull()
  }

  private fun postResult(context: Context, onResult: ((Result) -> Unit)?, result: Result) {
    Handler(Looper.getMainLooper()).post {
      onResult?.invoke(result)
      result.message?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      }
    }
  }
}

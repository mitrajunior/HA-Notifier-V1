package com.example.hanotifier.notify

import android.os.Bundle
import android.util.Patterns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import coil.compose.AsyncImage
import com.example.hanotifier.data.Action as NotificationAction
import com.example.hanotifier.ui.theme.AppTheme
import java.util.ArrayList

class AlertActivity : ComponentActivity() {

  companion object {
    const val EXTRA_TITLE = "com.example.hanotifier.extra.TITLE"
    const val EXTRA_BODY = "com.example.hanotifier.extra.BODY"
    const val EXTRA_IMAGE = "com.example.hanotifier.extra.IMAGE"
    const val EXTRA_ACTIONS = "com.example.hanotifier.extra.ACTIONS"
    const val EXTRA_NOTIFICATION_ID = "com.example.hanotifier.extra.NOTIFICATION_ID"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(
      WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
    )

    val title = intent.getStringExtra(EXTRA_TITLE) ?: "Alerta"
    val body = intent.getStringExtra(EXTRA_BODY) ?: ""
    val image = intent.getStringExtra(EXTRA_IMAGE)
    @Suppress("UNCHECKED_CAST")
    val actions = (intent.getSerializableExtra(EXTRA_ACTIONS) as? ArrayList<NotificationAction>)?.toList().orEmpty()
    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
    val appContext = applicationContext
    val activity = this

    setContent {
      AppTheme {
        AlertContent(
          title = title,
          body = body,
          image = image,
          actions = actions,
          onAction = { action ->
            ActionExecutor.execute(appContext, action) { result ->
              if (result.success) {
                if (notificationId != 0) {
                  NotificationManagerCompat.from(activity).cancel(notificationId)
                }
                activity.finish()
              }
            }
          },
          onLink = { url ->
            ActionExecutor.openLink(appContext, url) { result ->
              if (result.success) {
                if (notificationId != 0) {
                  NotificationManagerCompat.from(activity).cancel(notificationId)
                }
                activity.finish()
              }
            }
          },
          onAck = {
            if (notificationId != 0) {
              NotificationManagerCompat.from(activity).cancel(notificationId)
            }
            activity.finish()
          }
        )
      }
    }
  }
}

@Composable
private fun AlertContent(
  title: String,
  body: String,
  image: String?,
  actions: List<NotificationAction>,
  onAction: (NotificationAction) -> Unit,
  onLink: (String) -> Unit,
  onAck: () -> Unit,
) {
  val linkColor = MaterialTheme.colorScheme.primary
  val annotatedBody = remember(body, linkColor) {
    val matcher = Patterns.WEB_URL.matcher(body)
    if (!matcher.find()) {
      AnnotatedString(body)
    } else {
      matcher.reset()
      buildAnnotatedString {
        var lastIndex = 0
        while (matcher.find()) {
          val start = matcher.start()
          val end = matcher.end()
          append(body.substring(lastIndex, start))
          val url = matcher.group()
          val normalized = if (url.startsWith("http", true)) url else "https://$url"
          pushStringAnnotation(tag = "link", annotation = normalized)
          withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            append(url)
          }
          pop()
          lastIndex = end
        }
        append(body.substring(lastIndex))
      }
    }
  }

  Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Card(
        modifier = Modifier
          .padding(24.dp)
          .fillMaxWidth(0.92f)
          .widthIn(max = 480.dp),
        shape = RoundedCornerShape(20.dp)
      ) {
        val scrollState = rememberScrollState()
        Column(
          modifier = Modifier
            .padding(24.dp)
            .verticalScroll(scrollState),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(title, style = MaterialTheme.typography.headlineSmall)
          if (body.isNotBlank()) {
            ClickableText(
              text = annotatedBody,
              style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
              onClick = { offset ->
                annotatedBody.getStringAnnotations("link", offset, offset).firstOrNull()?.let { onLink(it.item) }
              }
            )
          }
          image?.takeIf { it.isNotBlank() }?.let { model ->
            AsyncImage(
              model = model,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .clip(RoundedCornerShape(16.dp))
            )
          }
          if (actions.isNotEmpty()) {
            Divider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              actions.forEachIndexed { index, action ->
                val buttonModifier = Modifier.fillMaxWidth()
                if (index == 0) {
                  Button(modifier = buttonModifier, onClick = { onAction(action) }) {
                    Text(action.title)
                  }
                } else {
                  OutlinedButton(modifier = buttonModifier, onClick = { onAction(action) }) {
                    Text(action.title)
                  }
                }
              }
            }
          }
          Spacer(modifier = Modifier.heightIn(min = 4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            OutlinedButton(onClick = onAck) { Text("Reconhecer") }
          }
        }
      }
    }
  }
}

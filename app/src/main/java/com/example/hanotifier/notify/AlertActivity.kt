package com.example.hanotifier.notify

import android.os.Bundle
import android.view.WindowManager
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
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
  val subtitle = when {
    actions.isEmpty() -> "Notificação recebida"
    actions.size == 1 -> "1 ação disponível"
    else -> "${actions.size} ações disponíveis"
  }

  Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Card(
        modifier = Modifier
          .padding(horizontal = 24.dp, vertical = 32.dp)
          .fillMaxWidth(0.92f)
          .widthIn(max = 520.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
      ) {
        Column {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.verticalGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                  )
                )
              )
              .padding(horizontal = 24.dp, vertical = 24.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
              )
              Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary
              )
              Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
              )
            }
          }

          val scrollState = rememberScrollState()
          Column(
            modifier = Modifier
              .padding(horizontal = 24.dp, vertical = 20.dp)
              .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
          ) {
            if (body.isNotBlank()) {
              MarkdownText(
                text = body,
                onLink = onLink,
                modifier = Modifier.fillMaxWidth()
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
                  .clip(RoundedCornerShape(18.dp))
              )
            }
            if (actions.isNotEmpty()) {
              Divider()
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                  text = "Ações rápidas",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary
                )
                actions.firstOrNull()?.let { primary ->
                  Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAction(primary) }
                  ) {
                    Text(primary.title)
                  }
                }
                actions.drop(1).forEach { action ->
                  FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAction(action) }
                  ) {
                    Text(action.title)
                  }
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(onClick = onAck) {
                Icon(
                  imageVector = Icons.Rounded.Done,
                  contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reconhecer")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MarkdownText(
  text: String,
  onLink: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val onLinkState = rememberUpdatedState(onLink)
  val markwon = remember(context) {
    MarkdownRenderer.withLinkResolver(context) { link -> onLinkState.value(link) }
  }
  val textColor = MaterialTheme.colorScheme.onSurface
  val linkColor = MaterialTheme.colorScheme.primary
  val typography = MaterialTheme.typography.bodyLarge
  val density = LocalDensity.current
  val fontSize = typography.fontSize
  val lineHeight = typography.lineHeight
  val lineSpacingExtra = remember(fontSize, lineHeight, density) {
    if (lineHeight.isSpecified && fontSize.isSpecified) {
      with(density) { lineHeight.toPx() - fontSize.toPx() }
    } else {
      0f
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      TextView(ctx).apply {
        setTextColor(textColor.toArgb())
        setLinkTextColor(linkColor.toArgb())
        if (fontSize.isSpecified) {
          setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.value)
        }
        if (lineSpacingExtra > 0f) {
          setLineSpacing(lineSpacingExtra, 1f)
        }
        movementMethod = LinkMovementMethod.getInstance()
      }
    },
    update = { view ->
      view.setTextColor(textColor.toArgb())
      view.setLinkTextColor(linkColor.toArgb())
      if (fontSize.isSpecified) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.value)
      }
      if (lineSpacingExtra > 0f) {
        view.setLineSpacing(lineSpacingExtra, 1f)
      }
      view.movementMethod = LinkMovementMethod.getInstance()
      MarkdownRenderer.setMarkdown(markwon, view, text)
    }
  )
}

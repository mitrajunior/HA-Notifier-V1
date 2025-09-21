package com.example.hanotifier.notify

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.widget.TextView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration

object MarkdownRenderer {

  @Volatile
  private var cachedMarkwon: Markwon? = null

  private fun baseMarkwon(context: Context): Markwon {
    val appContext = context.applicationContext
    val existing = cachedMarkwon
    if (existing != null) return existing
    return synchronized(this) {
      val current = cachedMarkwon
      current ?: Markwon.builder(appContext).build().also { cachedMarkwon = it }
    }
  }

  fun render(context: Context, markdown: String): Spanned {
    if (markdown.isBlank()) return SpannableString("")
    return baseMarkwon(context).toMarkdown(markdown)
  }

  fun setMarkdown(markwon: Markwon, view: TextView, markdown: String) {
    markwon.setMarkdown(view, markdown)
  }

  fun setMarkdown(view: TextView, markdown: String) {
    baseMarkwon(view.context).setMarkdown(view, markdown)
  }

  fun withLinkResolver(context: Context, resolver: (String) -> Unit): Markwon {
    val appContext = context.applicationContext
    return Markwon.builder(appContext)
      .usePlugin(object : AbstractMarkwonPlugin() {
        override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
          builder.linkResolver { _, link -> resolver(link) }
        }
      })
      .build()
  }
}

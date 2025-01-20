package com.github.premnirmal.ticker.ui

import android.content.Context
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.github.premnirmal.ticker.CustomTabs

data class LinkTextData(
  val text: String,
  val tag: String? = null,
  val annotation: String? = null,
  val onClick: ((context: Context, str: AnnotatedString.Range<String>) -> Unit)? = null,
)

@Composable
fun LinkText(
  linkTextData: List<LinkTextData>,
  modifier: Modifier = Modifier,
) {
  val annotatedString = createAnnotatedString(linkTextData)
  val context = LocalContext.current
  ClickableText(
      text = annotatedString,
      style = MaterialTheme.typography.bodySmall,
      onClick = { offset ->
        linkTextData.forEach { annotatedStringData ->
          if (annotatedStringData.tag != null && annotatedStringData.annotation != null) {
            annotatedString.getStringAnnotations(
                tag = annotatedStringData.tag,
                start = offset,
                end = offset,
            ).firstOrNull()?.let {
              annotatedStringData.onClick?.invoke(context, it) ?: CustomTabs.openTab(context, it.item)
            }
          }
        }
      },
      modifier = modifier,
  )
}

@Composable
private fun createAnnotatedString(data: List<LinkTextData>): AnnotatedString {
  return buildAnnotatedString {
    data.forEach { linkTextData ->
      if (linkTextData.tag != null && linkTextData.annotation != null) {
        pushStringAnnotation(
            tag = linkTextData.tag,
            annotation = linkTextData.annotation,
        )
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            ),
        ) {
          append(linkTextData.text)
        }
        pop()
      } else {
        append(linkTextData.text)
      }
    }
  }
}
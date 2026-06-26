package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * One run of text within a [LinkText]. When both [tag] and [annotation] are set the run is rendered
 * as a tappable link carrying [annotation] (typically the URL); otherwise it is plain text.
 */
data class LinkTextData(
    val text: String,
    val tag: String? = null,
    val annotation: String? = null,
)

/**
 * Shared (Compose Multiplatform) inline rich text that renders tappable links. The actual link
 * action is hoisted as [onLinkClick] (the tapped run's [LinkTextData.annotation]) because opening a
 * URL is platform specific — Android uses Chrome Custom Tabs, iOS its in-app browser — mirroring the
 * way the shared `NewsCard` hoists its tap handler.
 */
@Composable
fun LinkText(
    linkTextData: List<LinkTextData>,
    modifier: Modifier = Modifier,
    onLinkClick: (annotation: String) -> Unit = {},
) {
    val annotatedString = createAnnotatedString(linkTextData)
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
                        onLinkClick(it.item)
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

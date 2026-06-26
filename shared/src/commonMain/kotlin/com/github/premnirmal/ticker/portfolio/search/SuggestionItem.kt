package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_add_to_list
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.tickerwidget.ui.Divider
import org.jetbrains.compose.resources.painterResource

/**
 * Shared (Compose Multiplatform) search-suggestion row rendered identically on Android and iOS: the
 * suggestion label (tappable to open the quote) plus a trailing add/remove [IconButton] and a thin
 * [Divider]. The trailing affordance is configurable through [addRemoveIcon] /
 * [addRemoveContentDescription] / [addRemoveIconTint] so each platform can show its own icon
 * (Android opens the widget picker, iOS toggles watchlist membership).
 */
@Composable
fun SuggestionItem(
    suggestion: Suggestion,
    onSuggestionClick: (Suggestion) -> Unit,
    onSuggestionAddRemoveClick: (Suggestion) -> Unit,
    modifier: Modifier = Modifier,
    addRemoveIcon: Painter = painterResource(Res.drawable.ic_add_to_list),
    addRemoveContentDescription: String? = null,
    addRemoveIconTint: Color = LocalContentColor.current,
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSuggestionClick(suggestion) },
                text = suggestion.displayString(),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            )
            IconButton(
                onClick = {
                    onSuggestionAddRemoveClick(suggestion)
                }
            ) {
                Icon(
                    painter = addRemoveIcon,
                    contentDescription = addRemoveContentDescription,
                    tint = addRemoveIconTint,
                )
            }
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, end = 4.dp, start = 4.dp)
        )
    }
}

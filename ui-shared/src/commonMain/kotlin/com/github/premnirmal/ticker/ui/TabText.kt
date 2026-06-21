package com.github.premnirmal.ticker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

/**
 * A [Tab] whose label is rendered with the watchlist tab-row styling: a centered `labelMedium`
 * that becomes extra-bold (and switches to the on-primary-container colour) when [selected]. It is
 * built entirely from multiplatform `material3` APIs (no Android coupling), so it lives in
 * `:shared` `commonMain` and the shared watchlist UI (and iOS) can reuse it.
 */
@Composable
fun TabText(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            Text(
                text = text,
                style = if (selected) {
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    )
                } else {
                    MaterialTheme.typography.labelMedium
                },
                textAlign = TextAlign.Center,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
        }
    )
}

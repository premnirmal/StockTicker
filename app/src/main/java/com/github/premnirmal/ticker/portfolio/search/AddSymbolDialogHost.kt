package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.github.premnirmal.tickerwidget.ui.theme.AppTheme
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Thin Android host for the shared [AddSymbolDialogContent]. Resolves the Koin
 * `SuggestionViewModel` and forwards its state and add/remove events to the shared dialog.
 */
@Composable
fun AddSymbolDialog(
    symbol: String,
    onDismissRequest: () -> Unit = {},
) {
    val viewModel = koinViewModel<SuggestionViewModel>(key = symbol) { parametersOf(symbol) }
    val suggestionState by viewModel.suggestionState.collectAsState(SuggestionState(symbol, emptyList()))
    AddSymbolDialogContent(
        suggestionState = suggestionState,
        onDismissRequest = onDismissRequest,
        onRemoved = { suggestionWidgetData ->
            viewModel.removeFromWidget(suggestionWidgetData)
        },
        onAdded = { suggestionWidgetData ->
            viewModel.addToWidget(suggestionWidgetData)
        },
    )
}

@Preview
@Composable
fun AddSymbolScreenPreview() {
    AppTheme(theme = SelectedTheme.LIGHT) {
        AddSymbolDialogContent(
            suggestionState = SuggestionState(
                symbol = "AAPL",
                widgetDataList = listOf(
                    SuggestionWidgetDataState(
                        "AAPL",
                        "Widget #1",
                        1,
                        true
                    ),
                    SuggestionWidgetDataState(
                        "AAPL",
                        "Widget #2",
                        2,
                        false
                    ),
                )
            ),
            onAdded = { _ -> },
            onRemoved = { _ -> },
            onDismissRequest = {},
        )
    }
}

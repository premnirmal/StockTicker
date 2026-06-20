package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.string

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
        selectWidgetLabel = stringResource(id = string.select_widget),
        saveLabel = stringResource(id = string.save),
        addIcon = painterResource(R.drawable.ic_add_circle),
        removeIcon = painterResource(R.drawable.ic_remove_circle),
    )
}

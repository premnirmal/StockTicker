package com.github.premnirmal.ticker.portfolio.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SuggestionViewModel.Factory::class)
class SuggestionViewModel @AssistedInject constructor(
    @Assisted private val symbol: String,
    private val widgetDataProvider: WidgetDataProvider,
) : ViewModel() {

    val suggestionState: Flow<SuggestionState>
        get() = _suggestionState
    private val _suggestionState = MutableStateFlow(SuggestionState(symbol, emptyList()))

    init {
        viewModelScope.launch {
            widgetDataProvider.widgetData.collect { dataList ->
                _suggestionState.update {
                    it.copy(
                        widgetDataList = dataList.map { widgetData ->
                            SuggestionWidgetDataState(
                                symbol = symbol,
                                widgetName = widgetData.widgetName,
                                widgetId = widgetData.widgetId,
                                exists = widgetData.hasTicker(symbol),
                            )
                        }
                    )
                }
            }
        }
    }

    fun removeFromWidget(widgetDataState: SuggestionWidgetDataState) {
        widgetDataProvider.dataForWidgetId(widgetDataState.widgetId).removeStock(widgetDataState.symbol)
        _suggestionState.update {
            val widgetDataList = it.widgetDataList.toMutableList()
            widgetDataList[widgetDataList.indexOf(widgetDataState)] = widgetDataState.copy(
                exists = false
            )
            it.copy(
                widgetDataList = widgetDataList
            )
        }
    }

    fun addToWidget(widgetDataState: SuggestionWidgetDataState) {
        widgetDataProvider.dataForWidgetId(widgetDataState.widgetId).addTicker(widgetDataState.symbol)
        _suggestionState.update {
            val widgetDataList = it.widgetDataList.toMutableList()
            widgetDataList[widgetDataList.indexOf(widgetDataState)] = widgetDataState.copy(
                exists = true
            )
            it.copy(
                widgetDataList = widgetDataList
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(symbol: String): SuggestionViewModel
    }
}

data class SuggestionState(
    val symbol: String,
    val widgetDataList: List<SuggestionWidgetDataState>
)

data class SuggestionWidgetDataState(
    val symbol: String,
    val widgetName: String,
    val widgetId: Int,
    val exists: Boolean,
)

package com.github.premnirmal.ticker.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.StocksStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
  private val stocksProvider: StocksProvider,
  private val stocksStorage: StocksStorage
) : ViewModel() {

  lateinit var symbol: String
  val quote: Quote?
    get() = stocksProvider.getStock(symbol)

  fun setNotes(notesText: String) {
    viewModelScope.launch {
      quote?.let {
        val properties = it.properties ?: Properties(
            symbol
        )
        it.properties = properties
        properties.notes = notesText
        stocksStorage.saveQuoteProperties(properties)
      }
    }
  }
}
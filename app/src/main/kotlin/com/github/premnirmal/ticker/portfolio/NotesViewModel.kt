package com.github.premnirmal.ticker.portfolio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.StocksStorage
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotesViewModel(application: Application) : AndroidViewModel(application) {

  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var stocksStorage: StocksStorage

  lateinit var symbol: String

  init {
    Injector.appComponent.inject(this)
  }

  fun getQuote(): Quote? {
    return stocksProvider.getStock(symbol)
  }

  fun setNotes(notesText: String) {
    viewModelScope.launch {
      val quote = getQuote()
      quote?.let {
        if (it.properties == null) {
          it.properties = Properties(symbol)
        }
        it.properties!!.notes = notesText
        stocksStorage.saveQuoteProperties(it.properties!!)
      }
    }
  }
}
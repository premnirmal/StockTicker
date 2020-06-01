package com.github.premnirmal.ticker.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.premnirmal.ticker.model.IStocksProvider

class TotalHoldingsViewModelFactory(private val stocksProvider: IStocksProvider) : ViewModelProvider.NewInstanceFactory() {
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    return TotalHoldingsViewModel(stocksProvider) as T
  }
}

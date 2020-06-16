package com.github.premnirmal.ticker.debug

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.repo.QuoteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File
import javax.inject.Inject

class DbViewerViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    private const val FILENAME = "db.html"
  }

  private val _showProgress = MutableLiveData<Boolean>()
  val showProgress: LiveData<Boolean>
      get() = _showProgress

  private val _htmlFile = MutableLiveData<File>()
  val htmlFile: LiveData<File>
      get() = _htmlFile

  @Inject lateinit var dao: QuoteDao

  init {
    Injector.appComponent.inject(this)
  }

  fun generateDatabaseHtml() {
    viewModelScope.launch(Dispatchers.IO) {
      _showProgress.postValue(true)
      val qt = StringBuilder()
          .append("""
            <h2>Quotes</h2>
            <table border="1">
            <tr>
            <td>#</td>
            <td>Symbol</td><td>Name</td><td>LastTradePrice</td>
            <td>Change</td><td>Change %</td><td>Exchange</td>
            <td>Currency</td><td>Desc</td>
            </tr>
            """)
      val ht = StringBuilder()
          .append("""
            <h2>Holdings</h2>
            <table border="1">
            <tr>
            <td>id</td><td>Symbol</td><td>Shares</td><td>Price</td>
            </tr>
            """)
      val stringBuilder = StringBuilder().also { sb ->
        sb.append("<html><body>")
        var count = 0
        dao.getQuotesWithHoldings()
            .forEach {
              val quote = it.quote
              qt.append("<tr>")
                  .append("<td>${++count}</td>")
                  .append("<td>${quote.symbol}</td>")
                  .append("<td>${quote.name}</td>")
                  .append("<td>${quote.lastTradePrice}</td>")
                  .append("<td>${quote.change}</td>")
                  .append("<td>${quote.changeInPercent}%</td>")
                  .append("<td>${quote.stockExchange}</td>")
                  .append("<td>${quote.currency}</td>")
                  .append("<td>${quote.description}</td>")
                  .append("</tr>")

              val holdings = it.holdings
              holdings.forEach { holding ->
                ht.append("<tr>")
                    .append("<td>${holding.id}</td>")
                    .append("<td>${holding.quoteSymbol}</td>")
                    .append("<td>${holding.shares}</td>")
                    .append("<td>${holding.price}</td>")
                    .append("</tr>")
                yield()
              }
              yield()
            }
        qt.append("</table>")
        ht.append("</table>")
        sb.append(qt)
            .append(ht)
            .append("</body></html>")
      }
      val file = File(getApplication<StocksApp>().cacheDir, FILENAME)
      if (!file.exists()) {
        file.createNewFile()
      } else {
        file.delete()
        file.createNewFile()
      }
      file.writeText(stringBuilder.toString(), Charsets.UTF_8)
      _htmlFile.postValue(file)
      _showProgress.postValue(false)
    }
  }
}

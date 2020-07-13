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
import java.util.Locale
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
          .append(
              """
            <h2>Quotes</h2>
            <table>
            <tr>
            <th>#</th>
            <th>Symbol</th><th>Name</th><th>Last&nbsp;trade&nbsp;price</th>
            <th>Change</th><th>Change %</th><th>Exchange</th>
            <th>Currency</th><th>Dividend</th>
            </tr>
            """
          )
      val ht = StringBuilder()
          .append(
              """
            <h2>Holdings</h2>
            <table>
            <tr>
            <th>id</th><th>Symbol</th><th>Shares</th><th>Price</th>
            </tr>
            """
          )
      val pt = StringBuilder()
          .append(
              """
            <h2>Properties</h2>
            <table>
            <tr>
            <th>id</th><th>Symbol</th><th>Notes</th><th>Alert&nbsp;Above</th><th>Alert&nbsp;Below</th>
            </tr>
            """
          )
      val stringBuilder = StringBuilder().also { sb ->
        sb.append(
            "<html><body>\n" +
                "    <style>\n" +
                "        table, th, td {\n" +
                "            border: 1px solid black;\n" +
                "            border-collapse: collapse;\n" +
                "            padding: 2px;\n" +
                "        }\n" +
                "        th {\n" +
                "          background-color: lightgray;\n" +
                "        }\n" +
                "    </style>"
        )
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
                  .append(
                      if (quote.annualDividendRate > 0.0f && quote.annualDividendYield > 0.0f) {
                        "<td>${quote.annualDividendRate} (${String.format(
                            Locale.ENGLISH, "%.2f", quote.annualDividendYield * 100
                        )}%)</td>"
                      } else {
                        "<td></td>"
                      }
                  )
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
              val properties = it.properties
              if (properties != null) {
                pt.append("<tr>")
                    .append("<td>${properties.id}</td>")
                    .append("<td>${properties.quoteSymbol}</td>")
                    .append("<td>${properties.notes}</td>")
                    .append("<td>${properties.alertAbove}</td>")
                    .append("<td>${properties.alertBelow}</td>")
                    .append("</tr>")
                yield()
              }
            }
        qt.append("</table>")
        ht.append("</table>")
        pt.append("</table>")
        sb.append(qt)
            .append(ht)
            .append(pt)
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

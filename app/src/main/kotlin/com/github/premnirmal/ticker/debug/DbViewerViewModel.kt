package com.github.premnirmal.ticker.debug

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.github.premnirmal.ticker.StocksApp
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.RefreshWorker
import com.github.premnirmal.ticker.notifications.DailySummaryNotificationWorker
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
      val quotesInfo = StringBuilder()
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
      val holdingsInfo = StringBuilder()
          .append(
              """
            <h2>Holdings</h2>
            <table>
            <tr>
            <th>id</th><th>Symbol</th><th>Shares</th><th>Price</th>
            </tr>
            """
          )
      val propsInfo = StringBuilder()
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
            """<html><body>
                    <style>
                        table, th, td {
                            border: 1px solid black;
                            border-collapse: collapse;
                            padding: 2px;
                        }
                        th {
                          background-color: lightgray;
                        }
                    </style>
          """
        )
        var count = 0
        dao.getQuotesWithHoldings()
            .forEach {
              val quote = it.quote
              quotesInfo.append("<tr>")
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
                holdingsInfo.append("<tr>")
                    .append("<td>${holding.id}</td>")
                    .append("<td>${holding.quoteSymbol}</td>")
                    .append("<td>${holding.shares}</td>")
                    .append("<td>${holding.price}</td>")
                    .append("</tr>")
                yield()
              }
              val properties = it.properties
              if (properties != null) {
                propsInfo.append("<tr>")
                    .append("<td>${properties.id}</td>")
                    .append("<td>${properties.quoteSymbol}</td>")
                    .append("<td>${properties.notes}</td>")
                    .append("<td>${properties.alertAbove}</td>")
                    .append("<td>${properties.alertBelow}</td>")
                    .append("</tr>")
                yield()
              }
            }
        quotesInfo.append("</table>")
        holdingsInfo.append("</table>")
        propsInfo.append("</table>")
        val workerInfo = extractWorkerInfo(getApplication<Application>())
        sb.append(quotesInfo)
            .append(holdingsInfo)
            .append(propsInfo)
            .append(workerInfo)
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

  private fun extractWorkerInfo(context: Context): StringBuilder {
    val sb = StringBuilder().append(
        """
            <h2>Scheduled Work</h2>
            <table>
            <tr>
            <th>Tag</th><th>State</th><th>RunAttemptCount</th>
            </tr>
            """
    )
    with(WorkManager.getInstance(context)) {
      pruneWork()
      val workInfos = ArrayList<WorkInfo>().apply {
        addAll(getWorkInfosByTag(RefreshWorker.TAG).get())
        addAll(getWorkInfosByTag(RefreshWorker.TAG_PERIODIC).get())
        addAll(getWorkInfosByTag(DailySummaryNotificationWorker.TAG).get())
      }
      for (wi in workInfos) {
        sb.append("<tr>")
            .append("<td>${wi.tags.minByOrNull { it.length }!!}</td>")
            .append("<td>${wi.state.name}</td>")
            .append("<td>${wi.runAttemptCount}</td>")
            .append("</tr>")

      }
    }
    sb.append("</table>")
    return sb
  }
}

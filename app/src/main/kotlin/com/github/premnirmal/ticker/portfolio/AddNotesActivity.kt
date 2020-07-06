package com.github.premnirmal.ticker.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_notes.addButton
import kotlinx.android.synthetic.main.activity_notes.notesInputEditText
import kotlinx.android.synthetic.main.activity_notes.tickerName
import kotlinx.android.synthetic.main.activity_notes.toolbar
import javax.inject.Inject

class AddNotesActivity : BaseActivity() {

  companion object {
    const val QUOTE = "QUOTE"
    const val TICKER = "TICKER"
  }

  @Inject internal lateinit var stocksProvider: IStocksProvider
  internal lateinit var ticker: String
  override val simpleName: String = "AddNotesActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_notes)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)!!
    } else {
      ticker = ""
      InAppMessage.showToast(this, R.string.error_symbol)
      finish()
      return
    }
    tickerName.text = ticker

    val quote = stocksProvider.getStock(ticker)
    if (quote?.properties != null) {
      notesInputEditText.setText(quote.properties!!.notes)
    }

    addButton.setOnClickListener { onUpdateClicked() }
  }

  private fun onUpdateClicked() {
    val notesText = notesInputEditText.text.toString()
    stocksProvider.upsertNotes(ticker, notesText)
    updateActivityResult()
    dismissKeyboard()
    finish()
  }

  private fun updateActivityResult() {
    val quote = checkNotNull(stocksProvider.getStock(ticker))
    val data = Intent()
    data.putExtra(QUOTE, quote)
    setResult(Activity.RESULT_OK, data)
  }
}
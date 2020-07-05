package com.github.premnirmal.ticker.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_notes.addButton
import kotlinx.android.synthetic.main.activity_notes.notesInputEditText
import kotlinx.android.synthetic.main.activity_notes.tickerName
import kotlinx.android.synthetic.main.activity_notes.toolbar

class AddNotesActivity : BaseActivity() {

  companion object {
    const val QUOTE = "QUOTE"
    const val TICKER = "TICKER"
  }

  override val simpleName: String = "AddNotesActivity"
  internal lateinit var ticker: String
  private lateinit var notesViewModel: NotesViewModel

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
    notesViewModel = ViewModelProvider(this, AndroidViewModelFactory.getInstance(application))
        .get(NotesViewModel::class.java)
    tickerName.text = ticker
    notesViewModel.symbol = ticker
    val quote = notesViewModel.getQuote()
    quote?.properties?.let {
      notesInputEditText.setText(it.notes)
    }
    addButton.setOnClickListener { onUpdateClicked() }
  }

  private fun onUpdateClicked() {
    val notesText = notesInputEditText.text.toString()
    notesViewModel.setNotes(notesText)
    updateActivityResult()
    dismissKeyboard()
    finish()
  }

  private fun updateActivityResult() {
    val quote = checkNotNull(notesViewModel.getQuote())
    val data = Intent()
    data.putExtra(QUOTE, quote)
    setResult(Activity.RESULT_OK, data)
  }
}
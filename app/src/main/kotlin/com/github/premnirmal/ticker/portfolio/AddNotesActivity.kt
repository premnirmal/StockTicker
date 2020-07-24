package com.github.premnirmal.ticker.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.dismissKeyboard
import com.github.premnirmal.tickerwidget.R
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
  private lateinit var viewModel: NotesViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_notes)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    toolbar.setOnMenuItemClickListener {
      onSaveClick()
      true
    }
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)!!
    } else {
      ticker = ""
      InAppMessage.showToast(this, R.string.error_symbol)
      finish()
      return
    }
    viewModel = ViewModelProvider(this).get(NotesViewModel::class.java)
    tickerName.text = ticker
    viewModel.symbol = ticker
    val quote = viewModel.quote
    quote?.properties?.let {
      notesInputEditText.setText(it.notes)
    }
  }

  override fun onResume() {
    super.onResume()
    notesInputEditText.requestFocus()
  }

  private fun onSaveClick() {
    val notesText = notesInputEditText.text.toString()
    viewModel.setNotes(notesText)
    updateActivityResult()
    dismissKeyboard()
    finish()
  }

  private fun updateActivityResult() {
    val quote = checkNotNull(viewModel.quote)
    val data = Intent()
    data.putExtra(QUOTE, quote)
    setResult(Activity.RESULT_OK, data)
  }
}
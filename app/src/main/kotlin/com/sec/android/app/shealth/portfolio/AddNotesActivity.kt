package com.sec.android.app.shealth.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.sec.android.app.shealth.base.BaseActivity
import com.sec.android.app.shealth.components.InAppMessage
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.dismissKeyboard
import com.sec.android.app.shealth.R
import kotlinx.android.synthetic.main.activity_notes.*

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
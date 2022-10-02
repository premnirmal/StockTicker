package com.github.premnirmal.ticker.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.dismissKeyboard
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.ActivityNotesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddNotesActivity : BaseActivity<ActivityNotesBinding>() {
	override val binding: (ActivityNotesBinding) by viewBinding(ActivityNotesBinding::inflate)

  companion object {
    const val TICKER = "TICKER"
  }

  override val simpleName: String = "AddNotesActivity"
  internal lateinit var ticker: String
  private val viewModel: NotesViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding.toolbar.setNavigationOnClickListener {
      finish()
    }
    binding.toolbar.setOnMenuItemClickListener {
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
    binding.tickerName.text = ticker
    viewModel.symbol = ticker
    val quote = viewModel.quote
    quote?.properties?.let {
      binding.notesInputEditText.setText(it.notes)
    }
  }

  override fun onResume() {
    super.onResume()
    binding.notesInputEditText.requestFocus()
  }

  private fun onSaveClick() {
    val notesText = binding.notesInputEditText.text.toString()
    viewModel.setNotes(notesText)
    updateActivityResult()
    dismissKeyboard()
    finish()
  }

  private fun updateActivityResult() {
    val data = Intent()
    setResult(Activity.RESULT_OK, data)
  }
}
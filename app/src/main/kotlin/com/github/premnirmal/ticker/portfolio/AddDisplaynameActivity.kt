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
import com.github.premnirmal.tickerwidget.databinding.ActivityDisplaynameBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddDisplaynameActivity : BaseActivity<ActivityDisplaynameBinding>() {
  override val binding: (ActivityDisplaynameBinding) by viewBinding(ActivityDisplaynameBinding::inflate)

  companion object {
    const val TICKER = "TICKER"
  }

  override val simpleName: String = "AddDisplaynameActivity"
  internal lateinit var ticker: String
  private val viewModel: DisplaynameViewModel by viewModels()

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
      binding.displaynameInputEditText.setText(it.displayname)
    }
  }

  override fun onResume() {
    super.onResume()
    binding.displaynameInputEditText.requestFocus()
  }

  private fun onSaveClick() {
    val displayname = binding.displaynameInputEditText.text.toString()
    viewModel.setDisplayname(displayname)
    updateActivityResult()
    dismissKeyboard()
    finish()
  }

  private fun updateActivityResult() {
    val data = Intent()
    setResult(Activity.RESULT_OK, data)
  }
}
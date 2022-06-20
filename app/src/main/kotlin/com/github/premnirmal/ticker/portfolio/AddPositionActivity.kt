package com.github.premnirmal.ticker.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.dismissKeyboard
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.ActivityPositionsBinding
import com.github.premnirmal.tickerwidget.databinding.LayoutPositionHoldingBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat

/**
 * Created by premnirmal on 2/25/16.
 */
class AddPositionActivity : BaseActivity<ActivityPositionsBinding>() {

  companion object {
    const val QUOTE = "QUOTE"
    const val TICKER = "TICKER"
  }

  internal lateinit var ticker: String
  override val simpleName: String = "AddPositionActivity"


  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    binding.toolbar.setNavigationOnClickListener {
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
    binding.tickerName.text = ticker

    binding.addButton.setOnClickListener { onAddClicked() }

    binding.positionsHolder.removeAllViews()
    val position = stocksProvider.getPosition(ticker)
    position?.let {
      for (holding in position.holdings) {
        addPositionView(holding)
      }
    }
    updateTotal()
  }

  private fun onAddClicked() {
    val sharesView = binding.shares
    val priceView = binding.price
    val priceText = priceView.text.toString()
    val sharesText = sharesView.text.toString()
    if (priceText.isNotEmpty() && sharesText.isNotEmpty()) {
      var price = 0f
      var shares = 0f
      var success = true
      try {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        price = numberFormat.parse(priceText)!!
            .toFloat()
      } catch (e: NumberFormatException) {
        binding.priceInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
      try {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        shares = numberFormat.parse(sharesText)!!
            .toFloat()
      } catch (e: NumberFormatException) {
        binding.sharesInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
      // Check for zero shares.
      if (success) {
        if (shares == 0.0f) {
          binding.sharesInputLayout.error = getString(R.string.invalid_number)
          success = false
        }
      }
      if (success) {
        binding.priceInputLayout.error = null
        binding.sharesInputLayout.error = null
        lifecycleScope.launch {
          val holding = stocksProvider.addHolding(ticker, shares, price)
          priceView.setText("")
          sharesView.setText("")
          addPositionView(holding)
          updateTotal()
          updateActivityResult()
        }
      }
    }
    dismissKeyboard()
  }

  private fun updateActivityResult() {
    val quote = checkNotNull(stocksProvider.getStock(ticker))
    val data = Intent()
    data.putExtra(QUOTE, quote)
    setResult(Activity.RESULT_OK, data)
  }

  private fun addPositionView(holding: Holding) {
    val positionBinding = LayoutPositionHoldingBinding.inflate(layoutInflater)
    val positionNumShares = positionBinding.positionShares
    val positionPrice = positionBinding.positionPrice
    val positionTotalValue = positionBinding.positionTotalValue
    positionNumShares.text = AppPreferences.DECIMAL_FORMAT.format(holding.shares)
    positionPrice.text = AppPreferences.DECIMAL_FORMAT.format(holding.price)
    positionTotalValue.text = AppPreferences.DECIMAL_FORMAT.format(holding.totalValue())
    binding.positionsHolder.addView(positionBinding.root)
    positionBinding.root.tag = holding
    // Remove entry when right side 'x' icon is clicked.
    positionBinding.removePosition.setOnClickListener {
      AlertDialog.Builder(this)
          .setTitle(R.string.remove)
          .setMessage(getString(R.string.remove_holding, "${holding.shares}@${holding.price}"))
          .setPositiveButton(R.string.remove) { dialog, _ ->
            lifecycleScope.launch {
              stocksProvider.removePosition(ticker, holding)
              binding.positionsHolder.removeView(positionBinding.root)
              updateTotal()
            }
            dialog.dismiss()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    }
  }

  private fun updateTotal() {
    val quote = checkNotNull(stocksProvider.getStock(ticker))
    binding.totalShares.text = quote.numSharesString()
    binding.averagePrice.text = quote.averagePositionPrice()
    binding.totalValue.text = quote.totalSpentString()
  }
}
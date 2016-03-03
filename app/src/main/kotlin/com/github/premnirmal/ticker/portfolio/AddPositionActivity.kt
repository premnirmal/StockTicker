package com.github.premnirmal.ticker.portfolio

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import com.github.premnirmal.ticker.BaseActivity
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.R
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
open class AddPositionActivity : BaseActivity() {

    companion object {
        const val TICKER = "TICKER"
    }

    @Inject
    lateinit internal var stocksProvider: IStocksProvider
    protected var ticker: String? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.inject(this)
        setContentView(R.layout.activity_positions)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ticker = intent.getStringExtra(TICKER)
        val name = findViewById(R.id.tickerName) as TextView
        name.text = ticker

        findViewById(R.id.doneButton).setOnClickListener { onDoneClicked() }

        findViewById(R.id.skipButton).setOnClickListener { skip() }
    }

    protected open fun skip() {
        finish()
    }

    protected fun onDoneClicked() {
        val sharesView = findViewById(R.id.shares) as EditText
        val priceView = findViewById(R.id.price) as EditText
        val priceText = priceView.text.toString()
        val sharesText = sharesView.text.toString()
        if (!priceText.isEmpty() && !sharesText.isEmpty()) {
            val price = java.lang.Float.parseFloat(priceText)
            val shares = java.lang.Float.parseFloat(sharesText)
            stocksProvider.addPosition(ticker, shares, price)
        }
        finish()
    }
}
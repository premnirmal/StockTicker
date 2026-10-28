package com.github.premnirmal.ticker.portfolio

import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.navigation.calculateContentAndNavigationType
import com.github.premnirmal.ticker.network.data.holdingsSum
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.tickerwidget.R
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.google.accompanist.adaptive.calculateDisplayFeatures
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.NumberFormat
import kotlin.getValue

class HoldingsActivity : BaseActivity() {
    override val simpleName: String
        get() = "HoldingsActivity"

    internal lateinit var ticker: String
    private val viewModel: AddPositionViewModel by viewModel()

    override fun create(savedInstanceState: Bundle?) {
        super.create(savedInstanceState)
        if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
            ticker = intent.getStringExtra(TICKER)!!
            viewModel.loadQuote(ticker)
        } else {
            ticker = ""
            appMessaging.sendSnackbar(R.string.error_symbol)
            setResult(RESULT_CANCELED)
            finish()
            return
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    override fun ShowContent() {
        val windowSizeClass = calculateWindowSizeClass(this)
        val displayFeatures = calculateDisplayFeatures(this)
        val contentType = calculateContentAndNavigationType(
            widthSizeClass = windowSizeClass.widthSizeClass,
            displayFeatures = displayFeatures
        ).second

        val position by viewModel.position.collectAsStateWithLifecycle()
        var holdings by remember(position.holdings) {
            mutableStateOf(position.holdings.toList())
        }
        val holdingsSum by remember(holdings) {
            derivedStateOf { holdings.holdingsSum() }
        }
        LaunchedEffect(ticker) {
            viewModel.addedHolding.collect {
                holdings = holdings.toMutableList().apply { add(it) }
                updateActivityResult()
            }
        }
        LaunchedEffect(ticker) {
            viewModel.removedHolding.collect {
                holdings = holdings.toMutableList().apply { remove(it) }
                updateActivityResult()
            }
        }

        AddPositionScreen(
            ticker = ticker,
            holdings = holdings,
            holdingsSum = holdingsSum,
            title = stringResource(R.string.add_position),
            sharesLabel = stringResource(R.string.number_of_shares),
            priceLabel = stringResource(R.string.price),
            addLabel = stringResource(R.string.add),
            currentPositionsLabel = stringResource(R.string.current_positions),
            sharesColumnLabel = stringResource(R.string.shares),
            priceColumnLabel = stringResource(R.string.price),
            valueColumnLabel = stringResource(R.string.value),
            removeContentDescription = stringResource(R.string.remove_holding),
            backIcon = painterResource(R.drawable.ic_arrow_back),
            removeIcon = painterResource(R.drawable.ic_close),
            snackbarHostState = LocalAppMessaging.current.snackbarHostState,
            formatNumber = { AppPreferences.DECIMAL_FORMAT.format(it) },
            onBack = { finish() },
            onAdd = { priceText, sharesText -> onAddClicked(priceText, sharesText) },
            onRemove = { viewModel.removeHolding(ticker, it) },
            twoPane = if (contentType == SINGLE_PANE) {
                null
            } else {
                { first, second ->
                    TwoPane(
                        modifier = Modifier,
                        strategy = HorizontalTwoPaneStrategy(splitFraction = 1f / 2f),
                        displayFeatures = displayFeatures,
                        foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
                        first = first,
                        second = second,
                    )
                }
            }
        )
    }

    private fun onAddClicked(
        priceText: String,
        sharesText: String,
    ): Pair<Boolean, Boolean> {
        var priceError = false
        var sharesError = false
        if (priceText.isNotEmpty() && sharesText.isNotEmpty()) {
            var price = 0f
            var shares = 0f
            try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                price = numberFormat.parse(priceText)!!.toFloat()
            } catch (e: NumberFormatException) {
                priceError = true
                appMessaging.sendSnackbar(R.string.invalid_number)
            }
            try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                shares = numberFormat.parse(sharesText)!!.toFloat()
            } catch (e: NumberFormatException) {
                sharesError = true
                appMessaging.sendSnackbar(R.string.invalid_number)
            }
            // Check for zero shares.
            if (!sharesError && !priceError) {
                if (shares == 0.0f) {
                    sharesError = true
                    appMessaging.sendSnackbar(R.string.invalid_number)
                }
            }
            if (!sharesError && !priceError) {
                viewModel.addHolding(ticker, shares, price)
            }
        } else {
            sharesError = true
            priceError = true
            appMessaging.sendSnackbar(R.string.invalid_number)
        }
        return Pair(priceError, sharesError)
    }

    private fun updateActivityResult() {
        val data = Intent().apply {
            putExtra(POSITIONS, viewModel.position.value)
        }
        setResult(RESULT_OK, data)
    }

    companion object {
        const val TICKER = "TICKER"
        const val POSITIONS = "POSITIONS"
    }
}

package com.github.premnirmal.ticker.portfolio

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.navigation.calculateContentAndNavigationType
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.HoldingSum
import com.github.premnirmal.ticker.network.data.holdingsSum
import com.github.premnirmal.ticker.ui.AppTextFieldDefaultColors
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.Divider
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.toImmutableList
import java.text.NumberFormat
import kotlin.getValue

@AndroidEntryPoint
class HoldingsActivity : BaseActivity() {
    override val simpleName: String
        get() = "HoldingsActivity"

    internal lateinit var ticker: String
    private val viewModel: AddPositionViewModel by viewModels()

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
        val contentType: ContentType = calculateContentAndNavigationType(
            widthSizeClass = windowSizeClass.widthSizeClass,
            displayFeatures = displayFeatures
        ).second

        val position by viewModel.position.collectAsStateWithLifecycle()
        var holdings by remember(position.holdings) {
            mutableStateOf(position.holdings.toImmutableList())
        }
        val holdingsSum by remember(holdings) {
            derivedStateOf {
                holdings.holdingsSum()
            }
        }
        LaunchedEffect(ticker) {
            viewModel.addedHolding.collect {
                holdings = holdings.toMutableList().apply {
                    add(it)
                }
                updateActivityResult()
            }
        }
        LaunchedEffect(ticker) {
            viewModel.removedHolding.collect {
                holdings = holdings.toMutableList().apply {
                    remove(it)
                }
                updateActivityResult()
            }
        }

        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                TopBar(
                    text = stringResource(R.string.add_position),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                finish()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = LocalAppMessaging.current.snackbarHostState)
            }
        ) { paddingValues ->
            if (contentType == SINGLE_PANE) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .align(Alignment.Center)
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            text = ticker,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        HoldingsInput()
                        CurrentHoldings(holdings, holdingsSum)
                    }
                }
            } else {
                TwoPane(
                    modifier = Modifier.padding(paddingValues),
                    strategy = HorizontalTwoPaneStrategy(
                        splitFraction = 1f / 2f,
                    ),
                    displayFeatures = displayFeatures,
                    foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
                    first = {
                        Box(
                            modifier = Modifier
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                                    .align(Alignment.Center)
                                    .padding(horizontal = 16.dp),
                            ) {
                                Text(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    text = ticker,
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                                HoldingsInput()
                            }
                        }
                    },
                    second = {
                        CurrentHoldings(holdings, holdingsSum)
                    }
                )
            }
        }
    }

    @Composable
    private fun HoldingsInput() {
        val focusManager = LocalFocusManager.current
        val decimalFormatter = remember {
            DecimalFormatter()
        }
        var sharesError by remember {
            mutableStateOf(false)
        }
        var priceError by remember {
            mutableStateOf(false)
        }
        var priceText by remember(ticker) {
            mutableStateOf("")
        }
        var sharesText by remember(ticker) {
            mutableStateOf("")
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextField(
                shape = com.github.premnirmal.ticker.ui.AppTextFieldShape,
                modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
                value = sharesText,
                maxLines = 1,
                singleLine = true,
                textStyle = TextStyle.Default.copy(
                    textAlign = TextAlign.End
                ),
                isError = sharesError,
                label = { Text(text = stringResource(R.string.number_of_shares)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                visualTransformation = DecimalInputVisualTransformation(decimalFormatter),
                onValueChange = {
                    sharesText = decimalFormatter.cleanup(it).take(MAX_VALUE_LENGTH)
                },
                colors = AppTextFieldDefaultColors,
            )
            TextField(
                shape = com.github.premnirmal.ticker.ui.AppTextFieldShape,
                modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
                value = priceText,
                maxLines = 1,
                singleLine = true,
                textStyle = TextStyle.Default.copy(
                    textAlign = TextAlign.End
                ),
                isError = priceError,
                label = { Text(text = stringResource(R.string.price)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                visualTransformation = DecimalInputVisualTransformation(decimalFormatter),
                onValueChange = {
                    priceText = decimalFormatter.cleanup(it).take(MAX_VALUE_LENGTH)
                },
                colors = AppTextFieldDefaultColors,
            )
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
                onClick = {
                    val pair = onAddClicked(priceText, sharesText)
                    priceError = pair.first
                    sharesError = pair.second
                    if (!priceError && !sharesError) {
                        priceText = ""
                        sharesText = ""
                        focusManager.clearFocus()
                    }
                },
            ) {
                Text(
                    text = stringResource(R.string.add).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    @Composable
    private fun CurrentHoldings(
        holdings: List<Holding>,
        holdingsSum: HoldingSum
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(R.string.current_positions),
                style = MaterialTheme.typography.labelLarge,
            )

            HoldingRow(
                modifier = Modifier,
                shares = stringResource(R.string.shares),
                price = stringResource(R.string.price),
                value = stringResource(R.string.value),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
            )
            LazyColumn(
                Modifier.padding(vertical = 8.dp),
                state = rememberLazyListState(),
            ) {
                items(
                    count = holdings.size,
                    key = { i -> holdings[i].id ?: i }
                ) { i ->
                    val holding = holdings[i]
                    HoldingRow(
                        modifier = Modifier.padding(bottom = 8.dp),
                        shares = AppPreferences.DECIMAL_FORMAT.format(holding.shares),
                        price = AppPreferences.DECIMAL_FORMAT.format(holding.price),
                        value = AppPreferences.DECIMAL_FORMAT.format(holding.totalValue()),
                        showRemoveButton = true,
                        onRemoveClick = {
                            viewModel.removeHolding(ticker, holding)
                        }
                    )
                }
                item {
                    Divider()
                }
                item {
                    HoldingRow(
                        modifier = Modifier.padding(top = 8.dp),
                        shares = AppPreferences.DECIMAL_FORMAT.format(holdingsSum.totalShares),
                        price = AppPreferences.DECIMAL_FORMAT.format(holdingsSum.averagePrice),
                        value = AppPreferences.DECIMAL_FORMAT.format(holdingsSum.totalPaidPrice),
                    )
                }
            }
        }
    }

    @Composable
    private fun HoldingRow(
        modifier: Modifier = Modifier,
        shares: String,
        price: String,
        value: String,
        style: TextStyle = MaterialTheme.typography.bodyLarge,
        showRemoveButton: Boolean = false,
        onRemoveClick: () -> Unit = {},
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = shares,
                style = style,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = price,
                style = style,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = value,
                style = style,
            )
            IconButton(
                enabled = showRemoveButton,
                onClick = onRemoveClick
            ) {
                if (showRemoveButton) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.remove_holding),
                    )
                }
            }
        }
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
        private const val MAX_VALUE_LENGTH = 12
    }
}

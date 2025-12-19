package com.github.premnirmal.ticker.portfolio

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.ui.AppTextFieldDefaultColors
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import kotlin.getValue

@AndroidEntryPoint
class AlertsActivity : BaseActivity() {
    override val simpleName: String
        get() = "AlertsActivity"

    internal lateinit var ticker: String
    private val viewModel: AlertsViewModel by viewModels()

    override fun create(savedInstanceState: Bundle?) {
        super.create(savedInstanceState)
        if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
            ticker = intent.getStringExtra(TICKER)!!
        } else {
            ticker = ""
            appMessaging.sendSnackbar(R.string.error_symbol)
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        viewModel.symbol = ticker
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ShowContent() {
        val quote = viewModel.quote
        val alertAbove = quote?.getAlertAbove() ?: 0f
        val alertBelow = quote?.getAlertBelow() ?: 0f
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                TopBar(
                    text = stringResource(R.string.alerts),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                finish()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
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
            Box(
                modifier = Modifier.padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = ticker,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    val decimalFormatter = remember {
                        DecimalFormatter()
                    }
                    var isErrorAlertAbove by remember {
                        mutableStateOf(false)
                    }
                    var isErrorAlertBelow by remember {
                        mutableStateOf(false)
                    }
                    var alertAboveText by remember(ticker) {
                        mutableStateOf(
                            if (alertAbove > 0f) alertAbove.toString() else ""
                        )
                    }
                    var alertBelowText by remember(ticker) {
                        mutableStateOf(
                            if (alertBelow > 0f) alertBelow.toString() else ""
                        )
                    }
                    TextField(
                        shape = com.github.premnirmal.ticker.ui.AppTextFieldShape,
                        modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
                        value = alertAboveText,
                        maxLines = 1,
                        singleLine = true,
                        textStyle = TextStyle.Default.copy(
                            textAlign = TextAlign.End
                        ),
                        isError = isErrorAlertAbove,
                        label = { Text(text = stringResource(R.string.alert_above)) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                        visualTransformation = DecimalInputVisualTransformation(decimalFormatter),
                        onValueChange = {
                            alertAboveText = decimalFormatter.cleanup(it).take(MAX_VALUE_LENGTH)
                        },
                        colors = AppTextFieldDefaultColors,
                    )
                    TextField(
                        shape = com.github.premnirmal.ticker.ui.AppTextFieldShape,
                        modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
                        value = alertBelowText,
                        maxLines = 1,
                        singleLine = true,
                        textStyle = TextStyle.Default.copy(
                            textAlign = TextAlign.End
                        ),
                        isError = isErrorAlertBelow,
                        label = { Text(text = stringResource(R.string.alert_below)) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                        visualTransformation = DecimalInputVisualTransformation(decimalFormatter),
                        onValueChange = {
                            alertBelowText = decimalFormatter.cleanup(it).take(MAX_VALUE_LENGTH)
                        },
                        colors = AppTextFieldDefaultColors,
                    )

                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = {
                            val pair = saveAlerts(alertAboveText, alertBelowText)
                            isErrorAlertAbove = pair.first
                            isErrorAlertBelow = pair.second
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.save).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }

    private fun saveAlerts(
        alertAboveText: String,
        alertBelowText: String,
    ): Pair<Boolean, Boolean> {
        var isErrorAlertAbove: Boolean
        var isErrorAlertBelow: Boolean
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        val alertAboveText = alertAboveText.ifEmpty { "0" }
        val alertBelowText = alertBelowText.ifEmpty { "0" }
        val alertAbove = try {
            val parsed = numberFormat.parse(alertAboveText)!!.toFloat()
            isErrorAlertAbove = false
            parsed
        } catch (e: NumberFormatException) {
            isErrorAlertAbove = true
            0f
        }
        val alertBelow = try {
            val parsed = numberFormat.parse(alertBelowText)!!.toFloat()
            isErrorAlertBelow = false
            parsed
        } catch (e: NumberFormatException) {
            isErrorAlertBelow = true
            0f
        }
        if (!isErrorAlertAbove && !isErrorAlertBelow) {
            if (alertAbove > 0 && alertBelow > alertAbove) {
                isErrorAlertAbove = true
                isErrorAlertBelow = true
                appMessaging.sendSnackbar(R.string.alert_below_error)
            } else {
                viewModel.setAlerts(alertAbove, alertBelow)
                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(ALERT_ABOVE, alertAbove)
                        putExtra(ALERT_BELOW, alertBelow)
                    }
                )
                finish()
            }
        }
        return Pair(isErrorAlertAbove, isErrorAlertBelow)
    }

    companion object {
        const val TICKER = "TICKER"
        const val ALERT_ABOVE = "ALERT_ABOVE"
        const val ALERT_BELOW = "ALERT_BELOW"
        private const val MAX_VALUE_LENGTH = 12
    }
}

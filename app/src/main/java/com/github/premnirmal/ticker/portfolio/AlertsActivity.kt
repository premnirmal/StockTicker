package com.github.premnirmal.ticker.portfolio

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.tickerwidget.R
import java.text.NumberFormat
import kotlin.getValue
import org.koin.androidx.viewmodel.ext.android.viewModel

class AlertsActivity : BaseActivity() {
    override val simpleName: String
        get() = "AlertsActivity"

    internal lateinit var ticker: String
    private val viewModel: AlertsViewModel by viewModel()

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

    @Composable
    override fun ShowContent() {
        val quote = viewModel.quote
        AlertsScreen(
            ticker = ticker,
            alertAbove = quote?.getAlertAbove() ?: 0f,
            alertBelow = quote?.getAlertBelow() ?: 0f,
            title = stringResource(R.string.alerts),
            alertAboveLabel = stringResource(R.string.alert_above),
            alertBelowLabel = stringResource(R.string.alert_below),
            saveLabel = stringResource(R.string.save),
            backIcon = painterResource(R.drawable.ic_arrow_back),
            snackbarHostState = LocalAppMessaging.current.snackbarHostState,
            onBack = { finish() },
            onSave = { aboveText, belowText -> saveAlerts(aboveText, belowText) }
        )
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
            val parsed = numberFormat.parse(alertAboveText)?.toFloat()
                ?: throw NumberFormatException("Unable to parse: $alertAboveText")
            isErrorAlertAbove = false
            parsed
        } catch (e: Exception) {
            isErrorAlertAbove = true
            0f
        }
        val alertBelow = try {
            val parsed = numberFormat.parse(alertBelowText)?.toFloat()
                ?: throw NumberFormatException("Unable to parse: $alertBelowText")
            isErrorAlertBelow = false
            parsed
        } catch (e: Exception) {
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
    }
}

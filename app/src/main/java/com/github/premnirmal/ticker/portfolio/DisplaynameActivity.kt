package com.github.premnirmal.ticker.portfolio

import android.content.Intent
import android.os.Bundle
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.tickerwidget.R
import kotlin.getValue

class DisplaynameActivity : BaseActivity() {

    companion object {
        const val TICKER = "TICKER"
        const val DISPLAYNAME = "DISPLAYNAME"
    }

    override val simpleName: String
        get() = "DisplaynameActivity"

    private val viewModel: DisplaynameViewModel by viewModel()
    internal lateinit var ticker: String

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
        DisplaynameScreen(
            viewModel = viewModel,
            ticker = ticker,
            title = stringResource(R.string.displayname),
            addDisplaynameLabel = stringResource(R.string.add_displayname),
            doneContentDescription = stringResource(R.string.done),
            backIcon = painterResource(R.drawable.ic_arrow_back),
            doneIcon = painterResource(R.drawable.ic_done),
            onBack = { finish() },
            onDone = { displaynameText ->
                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(DISPLAYNAME, displaynameText)
                    }
                )
                finish()
            }
        )
    }
}

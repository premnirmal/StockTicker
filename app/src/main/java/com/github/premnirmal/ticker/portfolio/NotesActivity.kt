package com.github.premnirmal.ticker.portfolio

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.tickerwidget.R
import kotlin.getValue
import org.koin.androidx.viewmodel.ext.android.viewModel

class NotesActivity : BaseActivity() {

    companion object {
        const val TICKER = "TICKER"
        const val NOTES = "NOTES"
    }

    override val simpleName: String
        get() = "NotesActivity"

    private val viewModel: NotesViewModel by viewModel()
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
        NotesScreen(
            ticker = ticker,
            viewModel = viewModel,
            title = stringResource(R.string.notes),
            addNotesLabel = stringResource(R.string.add_notes),
            doneContentDescription = stringResource(R.string.done),
            backIcon = painterResource(R.drawable.ic_arrow_back),
            doneIcon = painterResource(R.drawable.ic_done),
            snackbarHostState = LocalAppMessaging.current.snackbarHostState,
            onBack = { finish() },
            onDone = { notes ->
                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(NOTES, notes)
                    }
                )
                finish()
            }
        )
    }
}

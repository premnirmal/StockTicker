package com.github.premnirmal.ticker.portfolio

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class NotesActivity : BaseActivity() {

    companion object {
        const val TICKER = "TICKER"
        const val NOTES = "NOTES"
    }

    override val simpleName: String
        get() = "NotesActivity"

    private val viewModel: NotesViewModel by viewModels()
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ShowContent() {
        var notes by remember(ticker) {
            val text = viewModel.quote?.properties?.notes ?: ""
            mutableStateOf(
                TextFieldValue(
                    text = text,
                    selection = TextRange(text.length),
                )
            )
        }
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                TopBar(
                    text = stringResource(R.string.notes),
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
                    },
                    actions = {
                        IconButton(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            onClick = {
                                viewModel.setNotes(notes.text)
                                setResult(
                                    RESULT_OK,
                                    Intent().apply {
                                        putExtra(NOTES, notes.text)
                                    }
                                )
                                finish()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_done),
                                contentDescription = stringResource(R.string.done),
                            )
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = LocalAppMessaging.current.snackbarHostState)
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val focusRequester = remember { FocusRequester() }
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally),
                    text = ticker,
                    style = MaterialTheme.typography.headlineMedium,
                )
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(vertical = 16.dp)
                        .focusRequester(focusRequester)
                        .verticalScroll(rememberScrollState()),
                    value = notes,
                    label = { Text(text = stringResource(R.string.add_notes)) },
                    onValueChange = { notes = it },
                    colors = TextFieldDefaults.colors().copy(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

package com.github.premnirmal.ticker.portfolio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.ui.AppTextFieldDefaultColors
import com.github.premnirmal.ticker.ui.AppTextFieldShape
import com.github.premnirmal.ticker.ui.TopBar

/**
 * Per-ticker note editor, shared by Android and iOS. Android resources (the localised labels and the
 * back/done [Painter]s) and the navigation side effects (`finish()`/`setResult()`) are hoisted as
 * parameters so the screen has no platform dependencies; the Android [NotesActivity] supplies them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    ticker: String,
    viewModel: NotesViewModel,
    title: String,
    addNotesLabel: String,
    doneContentDescription: String,
    backIcon: Painter,
    doneIcon: Painter,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDone: (String) -> Unit,
) {
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
                text = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = backIcon,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            viewModel.setNotes(notes.text)
                            onDone(notes.text)
                        }
                    ) {
                        Icon(
                            painter = doneIcon,
                            contentDescription = doneContentDescription,
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                shape = AppTextFieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
                    .focusRequester(focusRequester)
                    .verticalScroll(rememberScrollState()),
                value = notes,
                label = { Text(text = addNotesLabel) },
                onValueChange = { notes = it },
                colors = AppTextFieldDefaultColors,
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}

/**
 * Per-ticker display-name editor, shared by Android and iOS. Like [NotesScreen], the Android
 * resources and navigation side effects are hoisted as parameters; [DisplaynameActivity] supplies
 * them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaynameScreen(
    ticker: String,
    viewModel: DisplaynameViewModel,
    title: String,
    addDisplaynameLabel: String,
    doneContentDescription: String,
    backIcon: Painter,
    doneIcon: Painter,
    onBack: () -> Unit,
    onDone: (String) -> Unit,
) {
    var displayname by remember(ticker) {
        val text = viewModel.quote?.properties?.displayname ?: ""
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
                text = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = backIcon,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            viewModel.setDisplayname(displayname.text)
                            onDone(displayname.text)
                        }
                    ) {
                        Icon(
                            painter = doneIcon,
                            contentDescription = doneContentDescription,
                        )
                    }
                }
            )
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
                value = displayname,
                label = { Text(text = addDisplaynameLabel) },
                onValueChange = { displayname = it },
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

private const val MAX_VALUE_LENGTH = 12

/**
 * Per-ticker price-alert editor, shared by Android and iOS. The localised strings and back [Painter]
 * are hoisted as parameters, and the parse/validate/persist of the entered values is delegated to
 * [onSave] (which returns the `above`/`below` error flags) so the locale-aware number parsing stays
 * on the host. [AlertsActivity] supplies these.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    ticker: String,
    alertAbove: Float,
    alertBelow: Float,
    title: String,
    alertAboveLabel: String,
    alertBelowLabel: String,
    saveLabel: String,
    backIcon: Painter,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: (aboveText: String, belowText: String) -> Pair<Boolean, Boolean>,
) {
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopBar(
                text = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = backIcon,
                            contentDescription = null,
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.Box(
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
                val decimalFormatter = remember { DecimalFormatter() }
                var isErrorAlertAbove by remember { mutableStateOf(false) }
                var isErrorAlertBelow by remember { mutableStateOf(false) }
                var alertAboveText by remember(ticker) {
                    mutableStateOf(
                        if (alertAbove > 0f) decimalFormatter.cleanup(alertAbove.toString()) else ""
                    )
                }
                var alertBelowText by remember(ticker) {
                    mutableStateOf(
                        if (alertBelow > 0f) decimalFormatter.cleanup(alertBelow.toString()) else ""
                    )
                }
                TextField(
                    shape = AppTextFieldShape,
                    modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
                    value = alertAboveText,
                    maxLines = 1,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle.Default.copy(
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    ),
                    isError = isErrorAlertAbove,
                    label = { Text(text = alertAboveLabel) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    visualTransformation = DecimalInputVisualTransformation(decimalFormatter),
                    onValueChange = {
                        alertAboveText = decimalFormatter.cleanup(it).take(MAX_VALUE_LENGTH)
                    },
                    colors = AppTextFieldDefaultColors,
                )
                TextField(
                    shape = AppTextFieldShape,
                    modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
                    value = alertBelowText,
                    maxLines = 1,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle.Default.copy(
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    ),
                    isError = isErrorAlertBelow,
                    label = { Text(text = alertBelowLabel) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    visualTransformation = DecimalInputVisualTransformation(decimalFormatter),
                    onValueChange = {
                        alertBelowText = decimalFormatter.cleanup(it).take(MAX_VALUE_LENGTH)
                    },
                    colors = AppTextFieldDefaultColors,
                )

                androidx.compose.material3.Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        val pair = onSave(alertAboveText, alertBelowText)
                        isErrorAlertAbove = pair.first
                        isErrorAlertBelow = pair.second
                    },
                ) {
                    Text(
                        text = saveLabel.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

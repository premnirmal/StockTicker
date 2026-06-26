package com.github.premnirmal.ticker.portfolio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.HoldingSum
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

/**
 * Per-ticker "add position" / holdings editor, shared by Android and iOS. The localised strings and
 * the back/remove [Painter]s are hoisted as parameters, the holdings number formatting is delegated
 * to [formatNumber], and the parse/validate/persist of the entered values is delegated to [onAdd]
 * (which returns the `price`/`shares` error flags) so the locale-aware number parsing stays on the
 * host. The optional [twoPane] slot lets the host supply an adaptive two-pane layout (Android uses
 * Accompanist `TwoPane`); when it is `null` the screen renders a single column. [HoldingsActivity]
 * supplies these.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPositionScreen(
    ticker: String,
    holdings: List<Holding>,
    holdingsSum: HoldingSum,
    title: String,
    sharesLabel: String,
    priceLabel: String,
    addLabel: String,
    currentPositionsLabel: String,
    sharesColumnLabel: String,
    priceColumnLabel: String,
    valueColumnLabel: String,
    removeContentDescription: String,
    backIcon: Painter,
    removeIcon: Painter,
    snackbarHostState: SnackbarHostState,
    formatNumber: (Float) -> String,
    onBack: () -> Unit,
    onAdd: (priceText: String, sharesText: String) -> Pair<Boolean, Boolean>,
    onRemove: (Holding) -> Unit,
    twoPane: (@Composable (first: @Composable () -> Unit, second: @Composable () -> Unit) -> Unit)? = null,
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
        val input: @Composable () -> Unit = {
            AddPositionInput(
                ticker = ticker,
                sharesLabel = sharesLabel,
                priceLabel = priceLabel,
                addLabel = addLabel,
                onAdd = onAdd,
            )
        }
        val currentHoldings: @Composable () -> Unit = {
            CurrentHoldings(
                holdings = holdings,
                holdingsSum = holdingsSum,
                currentPositionsLabel = currentPositionsLabel,
                sharesColumnLabel = sharesColumnLabel,
                priceColumnLabel = priceColumnLabel,
                valueColumnLabel = valueColumnLabel,
                removeContentDescription = removeContentDescription,
                removeIcon = removeIcon,
                formatNumber = formatNumber,
                onRemove = onRemove,
            )
        }
        if (twoPane == null) {
            androidx.compose.foundation.layout.Box(
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
                    input()
                    currentHoldings()
                }
            }
        } else {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(paddingValues)
            ) {
                twoPane(
                    {
                        Column(
                            modifier = Modifier.fillMaxSize()
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                text = ticker,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            input()
                        }
                    },
                    {
                        currentHoldings()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPositionInput(
    ticker: String,
    sharesLabel: String,
    priceLabel: String,
    addLabel: String,
    onAdd: (priceText: String, sharesText: String) -> Pair<Boolean, Boolean>,
) {
    val decimalFormatter = remember { DecimalFormatter() }
    var sharesError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var priceText by remember(ticker) { mutableStateOf("") }
    var sharesText by remember(ticker) { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextField(
            shape = AppTextFieldShape,
            modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
            value = sharesText,
            maxLines = 1,
            singleLine = true,
            textStyle = TextStyle.Default.copy(textAlign = TextAlign.End),
            isError = sharesError,
            label = { Text(text = sharesLabel) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            visualTransformation = DecimalInputVisualTransformation(decimalFormatter),
            onValueChange = {
                sharesText = decimalFormatter.cleanup(it).take(MAX_VALUE_LENGTH)
            },
            colors = AppTextFieldDefaultColors,
        )
        TextField(
            shape = AppTextFieldShape,
            modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
            value = priceText,
            maxLines = 1,
            singleLine = true,
            textStyle = TextStyle.Default.copy(textAlign = TextAlign.End),
            isError = priceError,
            label = { Text(text = priceLabel) },
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
                val pair = onAdd(priceText, sharesText)
                priceError = pair.first
                sharesError = pair.second
                if (!priceError && !sharesError) {
                    priceText = ""
                    sharesText = ""
                }
            },
        ) {
            Text(
                text = addLabel.uppercase(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun CurrentHoldings(
    holdings: List<Holding>,
    holdingsSum: HoldingSum,
    currentPositionsLabel: String,
    sharesColumnLabel: String,
    priceColumnLabel: String,
    valueColumnLabel: String,
    removeContentDescription: String,
    removeIcon: Painter,
    formatNumber: (Float) -> String,
    onRemove: (Holding) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(vertical = 16.dp),
            text = currentPositionsLabel,
            style = MaterialTheme.typography.labelLarge,
        )
        HoldingRow(
            modifier = Modifier,
            shares = sharesColumnLabel,
            price = priceColumnLabel,
            value = valueColumnLabel,
            removeContentDescription = removeContentDescription,
            removeIcon = removeIcon,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
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
                    shares = formatNumber(holding.shares),
                    price = formatNumber(holding.price),
                    value = formatNumber(holding.totalValue()),
                    removeContentDescription = removeContentDescription,
                    removeIcon = removeIcon,
                    showRemoveButton = true,
                    onRemoveClick = { onRemove(holding) }
                )
            }
            item {
                HorizontalDivider(thickness = 0.2.dp)
            }
            item {
                HoldingRow(
                    modifier = Modifier.padding(top = 8.dp),
                    shares = formatNumber(holdingsSum.totalShares),
                    price = formatNumber(holdingsSum.averagePrice),
                    value = formatNumber(holdingsSum.totalPaidPrice),
                    removeContentDescription = removeContentDescription,
                    removeIcon = removeIcon,
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
    removeContentDescription: String,
    removeIcon: Painter,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    showRemoveButton: Boolean = false,
    onRemoveClick: () -> Unit = {},
) {
    androidx.compose.foundation.layout.Row(
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
                    painter = removeIcon,
                    contentDescription = removeContentDescription,
                )
            }
        }
    }
}

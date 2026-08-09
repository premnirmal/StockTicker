package com.github.premnirmal.ticker.portfolio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.github.premnirmal.ticker.network.data.LedgerSummary
import com.github.premnirmal.ticker.network.data.Movement
import com.github.premnirmal.ticker.network.data.MovementType
import com.github.premnirmal.ticker.ui.AppTextFieldDefaultColors
import com.github.premnirmal.ticker.ui.AppTextFieldShape
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.ui.theme.SharedColours

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

/** All display strings for the position editor, provided by the platform host. */
data class PositionEditorStrings(
    val title: String,
    val sharesLabel: String,
    val priceLabel: String,
    val sellPriceLabel: String,
    val buyToggle: String,
    val sellToggle: String,
    val buyButton: String,
    val sellButton: String,
    val yourPositionLabel: String,
    val movementsLabel: String,
    val sharesColumnLabel: String,
    val priceColumnLabel: String,
    val valueColumnLabel: String,
    val gainColumnLabel: String,
    val realizedTotalLabel: String,
    val removeContentDescription: String,
)

/**
 * Per-ticker "add position" / sell-shares / movements editor, shared by Android and iOS. The
 * localised [strings] and the back/remove [Painter]s are hoisted as parameters, the number
 * formatting is delegated to [formatNumber], and the parse/validate/persist of the entered values
 * is delegated to [onBuy]/[onSell] (which return the `price`/`shares` error flags) so the
 * locale-aware number parsing stays on the host. The optional [twoPane] slot lets the host supply
 * an adaptive two-pane layout (Android uses Accompanist `TwoPane`); when it is `null` the screen
 * renders a single column. [HoldingsActivity] supplies these.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPositionScreen(
    ticker: String,
    movements: List<Movement>,
    summary: LedgerSummary,
    strings: PositionEditorStrings,
    backIcon: Painter,
    removeIcon: Painter,
    snackbarHostState: SnackbarHostState,
    formatNumber: (Float) -> String,
    onBack: () -> Unit,
    onBuy: (priceText: String, sharesText: String) -> Pair<Boolean, Boolean>,
    onSell: (priceText: String, sharesText: String) -> Pair<Boolean, Boolean>,
    onRemove: (Movement) -> Unit,
    twoPane: (@Composable (first: @Composable () -> Unit, second: @Composable () -> Unit) -> Unit)? = null,
) {
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopBar(
                text = strings.title,
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
            PositionInput(
                ticker = ticker,
                strings = strings,
                onBuy = onBuy,
                onSell = onSell,
            )
        }
        val positionAndMovements: @Composable () -> Unit = {
            PositionAndMovements(
                movements = movements,
                summary = summary,
                strings = strings,
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
                    positionAndMovements()
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
                        positionAndMovements()
                    }
                )
            }
        }
    }
}

/**
 * The shares/price input form plus the Buy/Sell [SingleChoiceSegmentedButtonRow] toggle that
 * decides whether the submit button routes to [onBuy] or [onSell].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PositionInput(
    ticker: String,
    strings: PositionEditorStrings,
    onBuy: (priceText: String, sharesText: String) -> Pair<Boolean, Boolean>,
    onSell: (priceText: String, sharesText: String) -> Pair<Boolean, Boolean>,
) {
    val decimalFormatter = remember { DecimalFormatter() }
    var sharesError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var priceText by remember(ticker) { mutableStateOf("") }
    var sharesText by remember(ticker) { mutableStateOf("") }
    var isSell by remember(ticker) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally),
        ) {
            SegmentedButton(
                selected = !isSell,
                onClick = { isSell = false },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text(text = strings.buyToggle)
            }
            SegmentedButton(
                selected = isSell,
                onClick = { isSell = true },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text(text = strings.sellToggle)
            }
        }
        TextField(
            shape = AppTextFieldShape,
            modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally),
            value = sharesText,
            maxLines = 1,
            singleLine = true,
            textStyle = TextStyle.Default.copy(textAlign = TextAlign.End),
            isError = sharesError,
            label = { Text(text = strings.sharesLabel) },
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
            label = { Text(text = if (isSell) strings.sellPriceLabel else strings.priceLabel) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            visualTransformation = DecimalInputVisualTransformation(decimalFormatter),
            onValueChange = {
                priceText = decimalFormatter.cleanup(it).take(MAX_VALUE_LENGTH)
            },
            colors = AppTextFieldDefaultColors,
        )
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
            colors = if (isSell) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            onClick = {
                val pair = if (isSell) onSell(priceText, sharesText) else onBuy(priceText, sharesText)
                priceError = pair.first
                sharesError = pair.second
                if (!priceError && !sharesError) {
                    priceText = ""
                    sharesText = ""
                }
            },
        ) {
            Text(
                text = (if (isSell) strings.sellButton else strings.buyButton).uppercase(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * The pool summary ("your position" — current shares/average price/cost basis) followed by the
 * newest-first movements table (one row per buy/sell, each sell showing its realized gain) and a
 * realized-total footer.
 */
@Composable
private fun PositionAndMovements(
    movements: List<Movement>,
    summary: LedgerSummary,
    strings: PositionEditorStrings,
    removeIcon: Painter,
    formatNumber: (Float) -> String,
    onRemove: (Movement) -> Unit,
) {
    val gainByMovementId = remember(summary) {
        summary.movementGains.associateBy { it.movement.id }
    }
    val reversedMovements = remember(movements) { movements.asReversed() }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(vertical = 16.dp),
            text = strings.yourPositionLabel,
            style = MaterialTheme.typography.labelLarge,
        )
        MovementRow(
            first = strings.sharesColumnLabel,
            second = strings.priceColumnLabel,
            third = strings.valueColumnLabel,
            removeContentDescription = strings.removeContentDescription,
            removeIcon = removeIcon,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        )
        MovementRow(
            modifier = Modifier.padding(bottom = 8.dp),
            first = formatNumber(summary.shares),
            second = formatNumber(summary.averagePrice),
            third = formatNumber(summary.costBasis),
            removeContentDescription = strings.removeContentDescription,
            removeIcon = removeIcon,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            text = strings.movementsLabel,
            style = MaterialTheme.typography.labelLarge,
        )
        LazyColumn(
            Modifier.padding(vertical = 8.dp),
            state = rememberLazyListState(),
        ) {
            item {
                MovementRow(
                    first = "",
                    second = "",
                    third = strings.gainColumnLabel,
                    removeContentDescription = strings.removeContentDescription,
                    removeIcon = removeIcon,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
            items(
                count = reversedMovements.size,
                key = { i -> reversedMovements[i].id ?: i }
            ) { i ->
                val movement = reversedMovements[i]
                val gain = gainByMovementId[movement.id]?.gain
                val isBuy = movement.type == MovementType.BUY
                MovementRow(
                    modifier = Modifier.padding(bottom = 8.dp),
                    first = if (isBuy) strings.buyToggle else strings.sellToggle,
                    firstStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    firstColor = if (isBuy) SharedColours.PositiveGreen else SharedColours.NegativeRed,
                    second = "${formatNumber(movement.shares)} @ ${formatNumber(movement.price)}",
                    third = gain?.let { formatSignedGain(it, formatNumber) } ?: NO_GAIN_PLACEHOLDER,
                    thirdColor = gainColor(gain),
                    removeContentDescription = strings.removeContentDescription,
                    removeIcon = removeIcon,
                    showRemoveButton = true,
                    onRemoveClick = { onRemove(movement) },
                )
            }
            item {
                HorizontalDivider(thickness = 0.2.dp)
            }
            item {
                RealizedTotalRow(
                    label = strings.realizedTotalLabel,
                    value = formatSignedGain(summary.realizedGain, formatNumber),
                    color = gainColor(summary.realizedGain),
                )
            }
        }
    }
}

private const val NO_GAIN_PLACEHOLDER = "—"

/**
 * Formats a realized gain with an explicit `+` for non-negative values, mirroring the app-wide
 * convention in `Quote.gainLossString()`/`realizedGainString()`/`changeStringWithSign()` — negative
 * values already carry their own `-` from [formatNumber].
 */
private fun formatSignedGain(gain: Float, formatNumber: (Float) -> String): String {
    val formatted = formatNumber(gain)
    return if (gain >= 0f) "+$formatted" else formatted
}

/** BUY movements (and the pool summary/header rows) carry no gain, so [gain] is `null` for them. */
@Composable
private fun gainColor(gain: Float?): Color = when {
    gain == null -> Color.Unspecified
    gain >= 0f -> SharedColours.PositiveGreen
    else -> SharedColours.NegativeRed
}

@Composable
private fun RealizedTotalRow(
    label: String,
    value: String,
    color: Color,
) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

/**
 * A generic three-column-plus-remove-button row, reused both for the "your position" header/summary
 * (shares/price/value) and for each movement in the movements table (type/shares@price/gain). The
 * DB entity `MovementRow` lives in `com.github.premnirmal.ticker.repo.data` and is never imported
 * here, so there is no name clash.
 */
@Composable
private fun MovementRow(
    modifier: Modifier = Modifier,
    first: String,
    second: String,
    third: String,
    removeContentDescription: String,
    removeIcon: Painter,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    firstStyle: TextStyle = style,
    firstColor: Color = Color.Unspecified,
    thirdColor: Color = Color.Unspecified,
    showRemoveButton: Boolean = false,
    onRemoveClick: () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = first,
            style = firstStyle,
            color = firstColor,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = second,
            style = style,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = third,
            style = style,
            color = thirdColor,
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

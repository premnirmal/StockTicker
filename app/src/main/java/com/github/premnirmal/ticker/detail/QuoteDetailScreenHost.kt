package com.github.premnirmal.ticker.detail

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.navigation.calculateContentAndNavigationType
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.changeColour
import com.github.premnirmal.ticker.news.NewsCard
import com.github.premnirmal.ticker.news.QuoteDetailViewModel
import com.github.premnirmal.ticker.portfolio.AlertsActivity
import com.github.premnirmal.ticker.portfolio.DisplaynameActivity
import com.github.premnirmal.ticker.portfolio.HoldingsActivity
import com.github.premnirmal.ticker.portfolio.NotesActivity
import com.github.premnirmal.ticker.portfolio.search.AddSymbolDialog
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.LinkText
import com.github.premnirmal.ticker.ui.LinkTextData
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.ticker.ui.fadingEdges
import com.github.premnirmal.ticker.ui.formatAxisDate
import com.github.premnirmal.ticker.ui.formatAxisHour
import com.github.premnirmal.ticker.ui.formatAxisValue
import com.github.premnirmal.ticker.ui.formatChartMarker
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.ui.AppCard
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Android host for the shared [com.github.premnirmal.ticker.detail.QuoteDetailScreen]. Resolves the
 * Koin [QuoteDetailViewModel]/[AppPreferences], collects the ViewModel state, derives the localised
 * [QuoteDetailItem] rows (`buildQuoteDetails`), the change/up/down [ColourPalette] colours, the
 * localised [QuoteDetailStrings], the `ic_refresh`/`ic_add_to_list`/`ic_edit` icons, the
 * `AppCard`/`NewsCard`/`AddSymbolDialog`/`LinkText` slots, the platform chart formatters, the
 * `RuntimeShader`-based [fadingEdges], the [AppMessaging] snackbar host, the per-section
 * `Holdings`/`Alerts`/`Notes`/`Displayname` activity-result launchers and the adaptive Accompanist
 * [TwoPane] layout, owns the `loadQuote`/`fetchAll`/`fetchQuoteInRealTime`/`reset` lifecycle and the
 * range-change chart fetch, then delegates to the shared screen.
 */
@Composable
fun QuoteDetailScreen(
    modifier: Modifier = Modifier,
    widthSizeClass: WindowWidthSizeClass,
    contentType: com.github.premnirmal.ticker.ui.ContentType?,
    displayFeatures: List<DisplayFeature>,
    quote: Quote,
    viewModel: QuoteDetailViewModel = koinViewModel()
) {
    val resolvedContentType = contentType
        ?: calculateContentAndNavigationType(
            widthSizeClass = widthSizeClass, displayFeatures = displayFeatures
        ).second
    val context = LocalContext.current
    val appPreferences = koinInject<AppPreferences>()
    val appMessaging = LocalAppMessaging.current

    val articles by viewModel.newsData.collectAsStateWithLifecycle()
    val quoteDetail by viewModel.quote.collectAsStateWithLifecycle(null)
    val currentQuote = quoteDetail?.dataSafe?.quote ?: quote
    val details = remember(quoteDetail) {
        quoteDetail?.takeIf { it.wasSuccessful }
            ?.let { buildQuoteDetails(it.data, context) }
            ?: emptyList()
    }
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val showAddRemoveTooltip by viewModel.showAddRemoveTooltip.collectAsStateWithLifecycle(true)
    val chartData by viewModel.data.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val graphError by viewModel.dataFetchError.collectAsStateWithLifecycle()
    var isInPortfolio by remember(currentQuote, currentQuote.position) {
        mutableStateOf(viewModel.isInPortfolio(currentQuote.symbol))
    }

    val changeColour = chartData?.changeColour ?: currentQuote.changeColour

    // Per-section editable state, updated by the activity-result launchers below.
    var holdings by remember(currentQuote.position) { mutableStateOf(currentQuote.position) }
    var alertAbove by remember(currentQuote.symbol) { mutableFloatStateOf(currentQuote.getAlertAbove()) }
    var alertBelow by remember(currentQuote.symbol) { mutableFloatStateOf(currentQuote.getAlertBelow()) }
    var notes by remember(currentQuote.properties) { mutableStateOf(currentQuote.properties?.notes ?: "") }
    var displayname by remember(currentQuote.properties) {
        mutableStateOf(currentQuote.properties?.displayname ?: "")
    }

    val holdingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            holdings = result.data?.getParcelableExtra(HoldingsActivity.POSITIONS) ?: holdings
        }
    }
    val alertsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            alertAbove = data?.getFloatExtra(AlertsActivity.ALERT_ABOVE, alertAbove) ?: alertAbove
            alertBelow = data?.getFloatExtra(AlertsActivity.ALERT_BELOW, alertBelow) ?: alertBelow
        }
    }
    val notesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            notes = result.data?.getStringExtra(NotesActivity.NOTES) ?: notes
        }
    }
    val displaynameLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            displayname = result.data?.getStringExtra(DisplaynameActivity.DISPLAYNAME) ?: displayname
        }
    }

    val strings = QuoteDetailStrings(
        addToPortfolio = stringResource(R.string.add_to_portfolio),
        graphFetchFailed = stringResource(R.string.graph_fetch_failed),
        rangeOneDay = stringResource(R.string.one_day_short),
        rangeTwoWeeks = stringResource(R.string.two_weeks_short),
        rangeOneMonth = stringResource(R.string.one_month_short),
        rangeThreeMonth = stringResource(R.string.three_month_short),
        rangeOneYear = stringResource(R.string.one_year_short),
        rangeFiveYears = stringResource(R.string.five_years_short),
        rangeMax = stringResource(R.string.max),
        positions = stringResource(R.string.positions),
        alerts = stringResource(R.string.alerts),
        notes = stringResource(R.string.notes),
        displayname = stringResource(R.string.displayname),
        shares = stringResource(R.string.shares),
        equityValue = stringResource(R.string.equity_value),
        averagePrice = stringResource(R.string.average_price),
        gainLoss = stringResource(R.string.gain_loss),
        dayChangeAmount = stringResource(R.string.day_change_amount),
        alertAbove = stringResource(R.string.alert_above),
        alertBelow = stringResource(R.string.alert_below),
    )

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { appMessaging.sendSnackbar(it) }
    }
    LaunchedEffect(currentQuote.symbol, range) {
        viewModel.fetchChartData(currentQuote.symbol, range)
    }

    QuoteDetailScreen(
        quote = currentQuote,
        chartData = chartData,
        changeColour = changeColour,
        upColor = ColourPalette.PositiveGreen,
        downColor = ColourPalette.NegativeRed,
        details = details,
        articles = articles.map { it.article },
        website = quoteDetail?.dataSafe?.quoteSummary?.assetProfile?.website,
        longBusinessSummary = quoteDetail?.dataSafe?.quoteSummary?.assetProfile?.longBusinessSummary,
        isInPortfolio = isInPortfolio,
        isRefreshing = isRefreshing,
        showAddRemoveTooltip = showAddRemoveTooltip,
        range = range,
        graphError = graphError != null,
        position = holdings,
        alertAbove = alertAbove,
        alertBelow = alertBelow,
        alertAboveText = appPreferences.selectedDecimalFormat.format(alertAbove),
        alertBelowText = appPreferences.selectedDecimalFormat.format(alertBelow),
        notes = notes,
        displayname = displayname,
        strings = strings,
        refreshIcon = painterResource(R.drawable.ic_refresh),
        addIcon = painterResource(R.drawable.ic_add_to_list),
        editIcon = painterResource(R.drawable.ic_edit),
        snackbarHostState = appMessaging.snackbarHostState,
        onRefresh = {
            if (!isRefreshing) {
                viewModel.fetchAll(currentQuote)
            }
        },
        onRangeSelected = { selected -> viewModel.range.value = selected },
        onAddRemoveTooltipShown = { viewModel.addRemoveTooltipShown() },
        onCardClick = { title, data -> appMessaging.sendBottomSheet(title, data) },
        onEditPositions = {
            holdingsLauncher.launch(
                Intent(context, HoldingsActivity::class.java)
                    .putExtra(HoldingsActivity.TICKER, currentQuote.symbol)
            )
        },
        onEditAlerts = {
            alertsLauncher.launch(
                Intent(context, AlertsActivity::class.java)
                    .putExtra(AlertsActivity.TICKER, currentQuote.symbol)
            )
        },
        onEditNotes = {
            notesLauncher.launch(
                Intent(context, NotesActivity::class.java)
                    .putExtra(NotesActivity.TICKER, currentQuote.symbol)
            )
        },
        onEditDisplayname = {
            displaynameLauncher.launch(
                Intent(context, DisplaynameActivity::class.java)
                    .putExtra(DisplaynameActivity.TICKER, currentQuote.symbol)
            )
        },
        hourAxisFormatter = ::formatAxisHour,
        dateAxisFormatter = ::formatAxisDate,
        valueAxisFormatter = ::formatAxisValue,
        markerFormatter = ::formatChartMarker,
        card = { cardModifier, onClick, content ->
            AppCard(modifier = cardModifier, onClick = onClick, content = content)
        },
        newsCard = { article -> NewsCard(item = article) },
        modifier = modifier,
        addSymbolDialog = { symbol, onDismissRequest ->
            AddSymbolDialog(symbol = symbol, onDismissRequest = onDismissRequest)
        },
        websiteLink = { website ->
            val linkContext = LocalContext.current
            val linkColor = MaterialTheme.colorScheme.primary
            LinkText(
                linkTextData = listOf(
                    LinkTextData(
                        text = website,
                        tag = website,
                        annotation = website
                    )
                ),
                onLinkClick = { annotation ->
                    CustomTabs.openTab(linkContext, annotation, linkColor.toArgb())
                }
            )
        },
        listFadingEdges = { state -> Modifier.fadingEdges(state) },
        twoPane = if (resolvedContentType == SINGLE_PANE) {
            null
        } else {
            { first, second ->
                TwoPane(
                    strategy = HorizontalTwoPaneStrategy(
                        splitFraction = 1f / 2f,
                    ),
                    displayFeatures = displayFeatures,
                    foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
                    first = first,
                    second = second,
                )
            }
        },
    )

    DisposableEffect(currentQuote.symbol) {
        viewModel.loadQuote(currentQuote.symbol)
        viewModel.fetchAll(currentQuote)
        viewModel.fetchQuoteInRealTime(currentQuote.symbol)
        onDispose {
            viewModel.reset()
        }
    }
}

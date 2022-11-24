package com.github.premnirmal.ticker.detail

import android.R.color
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells.Adaptive
import androidx.compose.foundation.lazy.grid.GridCells.Fixed
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.layout.DisplayFeature
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency.RIGHT
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition.OUTSIDE_CHART
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.HistoryProvider.Range
import com.github.premnirmal.ticker.navigation.CalculateContentAndNavigationType
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.news.NewsCard
import com.github.premnirmal.ticker.news.NewsFeedItem.ArticleNewsFeed
import com.github.premnirmal.ticker.news.QuoteDetailViewModel
import com.github.premnirmal.ticker.news.QuoteDetailViewModel.QuoteDetail
import com.github.premnirmal.ticker.news.QuoteDetailViewModel.QuoteWithSummary
import com.github.premnirmal.ticker.portfolio.AddAlertsActivity
import com.github.premnirmal.ticker.portfolio.AddNotesActivity
import com.github.premnirmal.ticker.portfolio.AddPositionActivity
import com.github.premnirmal.ticker.portfolio.search.AddSuggestionScreen
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.DateAxisFormatter
import com.github.premnirmal.ticker.ui.ErrorState
import com.github.premnirmal.ticker.ui.HourAxisFormatter
import com.github.premnirmal.ticker.ui.LinkText
import com.github.premnirmal.ticker.ui.LinkTextData
import com.github.premnirmal.ticker.ui.MultilineXAxisRenderer
import com.github.premnirmal.ticker.ui.TextMarkerView
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.ticker.ui.ValueAxisFormatter
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.R.dimen
import com.github.premnirmal.tickerwidget.R.string
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane

@Composable
fun QuoteDetailScreen(
  modifier: Modifier = Modifier,
  widthSizeClass: WindowWidthSizeClass,
  contentType: ContentType?,
  displayFeatures: List<DisplayFeature>,
  quote: Quote,
  viewModel: QuoteDetailViewModel = hiltViewModel()
) {
  val details = viewModel.details.collectAsState(initial = emptyList())
  val articles = viewModel.newsData.observeAsState(initial = null)
  val quoteDetail = viewModel.quote.observeAsState()

  QuoteDetailContent(
      modifier, widthSizeClass, contentType, displayFeatures, quote, viewModel, details, articles, quoteDetail
  )

  DisposableEffect(quote.symbol) {
    viewModel.loadQuote(quote.symbol)
    viewModel.fetchQuote(quote.symbol)
    viewModel.fetchNews(quote, truncate = false)
    viewModel.fetchQuoteInRealTime(quote.symbol)
    viewModel.fetchChartData(quote.symbol, Range.ONE_MONTH)
    onDispose {
      viewModel.clear()
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun QuoteDetailContent(
  modifier: Modifier = Modifier,
  widthSizeClass: WindowWidthSizeClass,
  contentType: ContentType?,
  displayFeatures: List<DisplayFeature>,
  quote: Quote,
  viewModel: QuoteDetailViewModel,
  details: State<List<QuoteDetail>>,
  articles: State<List<ArticleNewsFeed>?>,
  quoteDetail: State<FetchResult<QuoteWithSummary>?>
) {
  val contentType: ContentType = contentType
      ?: CalculateContentAndNavigationType(
          widthSizeClass = widthSizeClass, displayFeatures = displayFeatures
      ).second
  var showAddOrRemoveDialog by remember { mutableStateOf(false) }
  var isInPortfolio by remember { mutableStateOf(viewModel.isInPortfolio(quote.symbol)) }
  Scaffold(
      modifier = modifier
          .background(MaterialTheme.colorScheme.surface),
      topBar = {
        TopBar(
            text = quote.symbol,
            actions = {
              val resource = if (isInPortfolio) {
                painterResource(id = R.drawable.ic_remove_circle)
              } else {
                painterResource(id = R.drawable.ic_add_circle)
              }
              IconButton(onClick = {
                showAddOrRemoveDialog = true
              }) {
                Icon(painter = resource, contentDescription = null)
              }
            }
        )
      }
  ) { padding ->
    if (contentType == ContentType.SINGLE_PANE) {
      LazyVerticalGrid(
          modifier = Modifier.padding(horizontal = 8.dp),
          columns = Adaptive(150.dp),
          contentPadding = padding,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        quoteInfo(quote, quoteDetail, viewModel)
        quoteDetailsGrid(details)
        quotePositionsNotesAlerts(quote, quoteDetail, isInPortfolio)
        quoteBackground(quoteDetail)
        newsItems(articles)
        item {
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    } else {
      TwoPane(
          strategy = HorizontalTwoPaneStrategy(
              splitFraction = 1f / 2f,
          ),
          displayFeatures = displayFeatures,
          foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
          first = {
            LazyVerticalGrid(
                modifier = Modifier.padding(horizontal = 8.dp),
                columns = Adaptive(150.dp),
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              quoteInfo(quote, quoteDetail, viewModel)
              quoteBackground(quoteDetail)
              item {
                Spacer(modifier = Modifier.height(16.dp))
              }
            }
          },
          second = {
            LazyVerticalGrid(
                modifier = Modifier.padding(horizontal = 8.dp),
                columns = Fixed(1),
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              quoteDetailsGrid(details)
              quotePositionsNotesAlerts(quote, quoteDetail, isInPortfolio)
              newsItems(articles)
              item {
                Spacer(modifier = Modifier.height(16.dp))
              }
            }
          }
      )
    }
  }
  if (showAddOrRemoveDialog) {
    AddSuggestionScreen(suggestion = Suggestion(symbol = quote.symbol).apply {
      exists = viewModel.isInPortfolio(symbol)
    }, onChange = { suggestion, widgetId ->
      isInPortfolio = !suggestion.exists
      suggestion.exists = !suggestion.exists
      if (suggestion.exists) {
        viewModel.addTickerToWidget(suggestion.symbol, widgetId)
      } else {
        viewModel.removeStock(suggestion.symbol)
      }
      showAddOrRemoveDialog = false
    }, onDismissRequest = {
      showAddOrRemoveDialog = false
    }, widgetDataList = viewModel.getWidgetDataList())
  }
}

private fun LazyGridScope.quoteBackground(quoteDetail: State<FetchResult<QuoteWithSummary>?>) {
  val longBusinessSummary = quoteDetail.value?.dataSafe?.quoteSummary?.assetProfile?.longBusinessSummary
  val website = quoteDetail.value?.data?.quoteSummary?.assetProfile?.website
  if (!longBusinessSummary.isNullOrEmpty() || !website.isNullOrEmpty()) {
    item(span = {
      GridItemSpan(maxLineSpan)
    }) {
      Column {
        if (!website.isNullOrEmpty()) {
          LinkText(
              modifier = Modifier.padding(top = 8.dp),
              linkTextData = listOf(
                  LinkTextData(
                      text = website,
                      tag = website,
                      annotation = website
                  )
              )
          )
        }
        if (!longBusinessSummary.isNullOrEmpty()) {
          Text(
              modifier = Modifier.padding(top = 8.dp),
              text = longBusinessSummary,
              style = MaterialTheme.typography.bodySmall
          )
        }
      }
    }
  }
}

private fun LazyGridScope.quoteInfo(
  quote: Quote,
  quoteDetail: State<FetchResult<QuoteWithSummary>?>,
  viewModel: QuoteDetailViewModel
) {
  var quote: Quote = quote
  quoteDetail.value?.dataSafe?.quote?.let {
    quote = it
  }
  item(span = {
    GridItemSpan(maxLineSpan)
  }) {
    Text(
        text = quote.name,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
  }
  item(span = {
    GridItemSpan(maxLineSpan)
  }) {
    Text(
        text = quote.priceFormat.format(quote.lastTradePrice),
        style = MaterialTheme.typography.titleLarge,
        color = quote.changeColour,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
  }
  item(span = {
    GridItemSpan(maxLineSpan)
  }) {
    Row(horizontalArrangement = Arrangement.Center) {
      Text(
          modifier = Modifier.padding(end = 4.dp),
          text = quote.changeStringWithSign(),
          color = quote.changeColour,
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.End
      )
      Text(
          modifier = Modifier.padding(start = 4.dp),
          text = quote.changePercentStringWithSign(),
          color = quote.changeColour,
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Start
      )
    }
  }
  item(span = {
    GridItemSpan(maxLineSpan)
  }) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(200.dp), contentAlignment = Alignment.Center) {
      val graphData = viewModel.data.observeAsState()
      val graphError = viewModel.dataFetchError.observeAsState()
      val dataPoints = graphData.value
      if (graphError.value == null && dataPoints.isNullOrEmpty()) {
        CircularProgressIndicator()
      } else if (graphError.value != null && dataPoints.isNullOrEmpty()) {
        ErrorState(text = stringResource(id = string.graph_fetch_failed))
      } else {
        AndroidView(factory = { context ->
          createGraphView(context)
        }, update = { graphView ->
          updateGraphView(dataPoints, graphView, quote, Range.ONE_MONTH)
        })
      }
    }
  }
}

private fun LazyGridScope.quoteDetailsGrid(details: State<List<QuoteDetail>>) {
  item(span = {
    GridItemSpan(maxLineSpan)
  }) {
    val list = details.value
    if (list.isNotEmpty()) {
      val first = list.filterIndexed { index, _ ->
        index % 2 == 0
      }
      val second = list.filterIndexed { index, _ ->
        index % 2 != 0
      }
      Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(0.5f)) {
          first.forEach {
            Box(Modifier.padding(top = 4.dp, bottom = 4.dp, end = 4.dp)) {
              QuoteDetailCard(item = it)
            }
          }
        }
        Column(modifier = Modifier.weight(0.5f)) {
          second.forEach {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp, bottom = 4.dp, start = 4.dp)) {
              QuoteDetailCard(item = it)
            }
          }
        }
      }
    }
  }
}

private fun LazyGridScope.quotePositionsNotesAlerts(
  quote: Quote,
  quoteDetail: State<FetchResult<QuoteWithSummary>?>,
  isInPortfolio: Boolean
) {
  val quote: Quote = quoteDetail.value?.dataSafe?.quote ?: quote
  if (isInPortfolio) {
    item(span = {
    GridItemSpan(maxLineSpan)
  }) {
    val context = LocalContext.current
    val intent = Intent(context, AddPositionActivity::class.java)
    intent.putExtra(AddPositionActivity.TICKER, quote.symbol)
    EditSectionHeader(title = string.positions, intent = intent)
  }
    item(span = {
      GridItemSpan(maxLineSpan)
    }) {
      PositionDetailCard(quote)
    }
    item(span = {
      GridItemSpan(maxLineSpan)
    }) {
      val context = LocalContext.current
      val intent = Intent(context, AddAlertsActivity::class.java)
      intent.putExtra(AddAlertsActivity.TICKER, quote.symbol)
      EditSectionHeader(title = string.alerts, intent = intent)
    }
    item(span = {
      GridItemSpan(maxLineSpan)
    }) {
      AlertsCard(quote = quote)
    }
    item(span = {
      GridItemSpan(maxLineSpan)
    }) {
      val context = LocalContext.current
      val intent = Intent(context, AddNotesActivity::class.java)
      intent.putExtra(AddNotesActivity.TICKER, quote.symbol)
      EditSectionHeader(title = string.notes, intent = intent)
    }
    item(span = {
      GridItemSpan(maxLineSpan)
    }) {
      val context = LocalContext.current
      val intent = Intent(context, AddNotesActivity::class.java)
      intent.putExtra(AddNotesActivity.TICKER, quote.symbol)
      Text(
          modifier = Modifier
              .fillMaxWidth()
              .clickable { context.startActivity(intent) },
          text = if (!quote.properties?.notes.isNullOrEmpty()) quote.properties?.notes.orEmpty() else "--",
          style = MaterialTheme.typography.bodySmall,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis
      )
    }
  }
}

private fun LazyGridScope.newsItems(articles: State<List<ArticleNewsFeed>?>) {
  items(
      count = articles.value?.size ?: 0,
      span = {
        GridItemSpan(maxLineSpan)
      }) { i ->
    val item = articles.value!![i]
    NewsCard(item.article)
  }
}

private fun updateGraphView(
  dataPoints: List<DataPoint>?,
  graphView: LineChart,
  quote: Quote,
  range: Range
) {
  if (dataPoints.isNullOrEmpty()) {
    graphView.setNoDataText(graphView.context.getString(string.no_data))
    graphView.invalidate()
    return
  }
  graphView.setNoDataText("")
  graphView.lineData?.clearValues()
  val series = LineDataSet(dataPoints, quote.symbol)
  series.setDrawHorizontalHighlightIndicator(false)
  series.setDrawValues(false)
  val colorAccent = if (quote.changeInPercent >= 0) {
    ContextCompat.getColor(graphView.context, R.color.positive_green_dark)
  } else {
    ContextCompat.getColor(graphView.context, R.color.negative_red)
  }
  series.setDrawFilled(true)
  series.color = colorAccent
  series.fillColor = colorAccent
  series.fillAlpha = 150
  series.setDrawCircles(true)
  series.mode = CUBIC_BEZIER
  series.cubicIntensity = 0.07f
  series.lineWidth = 2f
  series.setDrawCircles(false)
  series.highLightColor = Color.GRAY
  val lineData = LineData(series)
  graphView.data = lineData
  val xAxis: XAxis = graphView.xAxis
  val yAxis: YAxis = graphView.axisRight
  if (range == Range.ONE_DAY) {
    xAxis.valueFormatter = HourAxisFormatter()
  } else {
    xAxis.valueFormatter = DateAxisFormatter()
  }
  yAxis.valueFormatter = ValueAxisFormatter()
  xAxis.position = BOTTOM
  xAxis.textSize = 10f
  yAxis.textSize = 10f
  xAxis.textColor = Color.GRAY
  yAxis.textColor = Color.GRAY
  xAxis.setLabelCount(5, true)
  yAxis.setLabelCount(5, true)
  yAxis.setPosition(OUTSIDE_CHART)
  xAxis.setDrawAxisLine(true)
  yAxis.setDrawAxisLine(true)
  xAxis.setDrawGridLines(false)
  yAxis.setDrawGridLines(false)
  graphView.invalidate()
}

private fun createGraphView(context: Context): LineChart {
  val graphView = LineChart(context)
  graphView.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
  graphView.isDoubleTapToZoomEnabled = false
  graphView.axisLeft.setDrawGridLines(false)
  graphView.axisLeft.setDrawAxisLine(false)
  graphView.axisLeft.isEnabled = false
  graphView.axisRight.setDrawGridLines(false)
  graphView.axisRight.setDrawAxisLine(true)
  graphView.axisRight.isEnabled = true
  graphView.xAxis.setDrawGridLines(false)
  graphView.setXAxisRenderer(
      MultilineXAxisRenderer(
          graphView.viewPortHandler, graphView.xAxis,
          graphView.getTransformer(RIGHT)
      )
  )
  graphView.extraBottomOffset =
    context.resources.getDimension(dimen.graph_bottom_offset)
  graphView.legend.isEnabled = false
  graphView.description = null
  val colorAccent = if (VERSION.SDK_INT >= VERSION_CODES.S) {
    ContextCompat.getColor(context, color.system_accent1_600)
  } else {
    ContextCompat.getColor(context, R.color.accent_fallback)
  }
  graphView.setNoDataText("")
  graphView.setNoDataTextColor(colorAccent)
  graphView.marker = TextMarkerView(context)
  return graphView
}

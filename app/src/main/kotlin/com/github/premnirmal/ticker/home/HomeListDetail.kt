package com.github.premnirmal.ticker.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import com.github.premnirmal.ticker.home.SelectionVisibilityState.NoSelection
import com.github.premnirmal.ticker.home.SelectionVisibilityState.ShowSelection
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.DevicePosture
import com.github.premnirmal.ticker.ui.ListDetail
import com.github.premnirmal.ticker.ui.NavigationContentPosition
import com.github.premnirmal.ticker.ui.NavigationType
import com.github.premnirmal.ticker.ui.isBookPosture
import com.github.premnirmal.ticker.ui.isSeparating
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeListDetail(
  windowSizeClass: WindowSizeClass,
  displayFeatures: List<DisplayFeature>,
  homeViewModel: HomeViewModel = viewModel()
) {
  HomeListDetail(windowSizeClass, displayFeatures, homeViewModel.portfolio)
}

@Composable
fun HomeListDetail(
  windowSizeClass: WindowSizeClass,
  displayFeatures: List<DisplayFeature>,
  quotesFlow: StateFlow<List<Quote>>
) {
  // Query for the current window size class
  val widthSizeClass by rememberUpdatedState(windowSizeClass.widthSizeClass)
  val heightSizeClass by rememberUpdatedState(windowSizeClass.heightSizeClass)
  /**
   * This will help us select type of navigation and content type depending on window size and
   * fold state of the device.
   */
  val navigationType: NavigationType
  val contentType: ContentType

  /**
   * We are using display's folding features to map the device postures a fold is in.
   * In the state of folding device If it's half fold in BookPosture we want to avoid content
   * at the crease/hinge
   */
  val foldingFeature = displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()

  val foldingDevicePosture = when {
    isBookPosture(foldingFeature) ->
      DevicePosture.BookPosture(foldingFeature.bounds)

    isSeparating(foldingFeature) ->
      DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)

    else -> DevicePosture.NormalPosture
  }

  when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> {
      navigationType = NavigationType.BOTTOM_NAVIGATION
      contentType = ContentType.SINGLE_PANE
    }
    WindowWidthSizeClass.Medium -> {
      navigationType = NavigationType.NAVIGATION_RAIL
      contentType = if (foldingDevicePosture != DevicePosture.NormalPosture) {
        ContentType.DUAL_PANE
      } else {
        ContentType.SINGLE_PANE
      }
    }
    WindowWidthSizeClass.Expanded -> {
      navigationType = NavigationType.NAVIGATION_RAIL
      contentType = ContentType.DUAL_PANE
    }
    else -> {
      navigationType = NavigationType.BOTTOM_NAVIGATION
      contentType = ContentType.SINGLE_PANE
    }
  }

  /**
   * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
   * ergonomics and reachability depending upon the height of the device.
   */
  val navigationContentPosition = when (heightSizeClass) {
    WindowHeightSizeClass.Compact -> {
      NavigationContentPosition.TOP
    }
    WindowHeightSizeClass.Medium,
    WindowHeightSizeClass.Expanded -> {
      NavigationContentPosition.CENTER
    }
    else -> {
      NavigationContentPosition.TOP
    }
  }

  HomeListDetailNavigationWrapper(
      widthSizeClass = widthSizeClass,
      navigationType = navigationType,
      contentType = contentType,
      displayFeatures = displayFeatures,
      quotesFlow = quotesFlow
  )
}

@Composable
private fun HomeListDetailNavigationWrapper(
  widthSizeClass: WindowWidthSizeClass,
  navigationType: NavigationType,
  contentType: ContentType,
  displayFeatures: List<DisplayFeature>,
  navigationContentPosition: NavigationContentPosition = NavigationContentPosition.TOP,
  quotesFlow: StateFlow<List<Quote>>
) {

  /**
   * The index of the currently selected quote.
   */
  var selectedQuote by rememberSaveable { mutableStateOf(-1) }

  /**
   * True if the detail is currently open. This is the primary control for "navigation".
   */
  var isDetailOpen by rememberSaveable { mutableStateOf(false) }

  val quotes by quotesFlow.collectAsState()

  ListDetail(
      isDetailOpen = isDetailOpen,
      setIsDetailOpen = {
        selectedQuote = -1
        isDetailOpen = it
      },
      showListAndDetail = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> false
        WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded -> quotes.isNotEmpty() && selectedQuote >= 0
        else -> quotes.isNotEmpty() && selectedQuote >= 0
      },
      detailKey = selectedQuote,
      list = { isDetailVisible ->
        ListContent(
            quotes = quotes,
            selectionState = if (isDetailVisible) {
              SelectionVisibilityState.ShowSelection(selectedQuote)
            } else {
              SelectionVisibilityState.NoSelection
            },
            onIndexClick = { index ->
              selectedQuote = index
              // Consider the detail to now be open. This acts like a navigation if
              // there isn't room for both list and detail, and also will result
              // in the detail remaining open in the case of resize.
              isDetailOpen = true
            },
            modifier = if (isDetailVisible) {
              Modifier.padding(end = 8.dp)
            } else {
              Modifier
            }
        )
      },
      detail = { isListVisible ->
        val quote = if (selectedQuote >= 0) quotes[selectedQuote] else null
        DetailContent(
            quote = quote,
            modifier = if (isListVisible) {
              Modifier.padding(start = 8.dp)
            } else {
              Modifier
            }
        )
      },
      twoPaneStrategy = HorizontalTwoPaneStrategy(
          splitFraction = 1f / 3f,
      ),
      displayFeatures = displayFeatures,
      modifier = Modifier.padding(horizontal = 16.dp)
  )
}

/**
 * The description of the selection state for the [ListContent]
 */
sealed interface SelectionVisibilityState {

  /**
   * No selection should be shown, and each item should be clickable.
   */
  object NoSelection : SelectionVisibilityState

  /**
   * Selection state should be shown, and each item should be selectable.
   */
  data class ShowSelection(
    /**
     * The index of the word that is selected.
     */
    val selectedWordIndex: Int
  ) : SelectionVisibilityState
}

/**
 * The content for the list pane.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListContent(
  quotes: List<Quote>,
  selectionState: SelectionVisibilityState,
  onIndexClick: (index: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
      contentPadding = PaddingValues(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = modifier
          .then(
              when (selectionState) {
                NoSelection -> Modifier
                is ShowSelection -> Modifier.selectableGroup()
              }
          )
  ) {
    itemsIndexed(quotes) { index, quote ->
      val interactionSource = remember { MutableInteractionSource() }

      val interactionModifier = when (selectionState) {
        NoSelection -> {
          Modifier.clickable(
              interactionSource = interactionSource,
              indication = rememberRipple(),
              onClick = { onIndexClick(index) }
          )
        }
        is ShowSelection -> {
          Modifier.selectable(
              selected = index == selectionState.selectedWordIndex,
              interactionSource = interactionSource,
              indication = rememberRipple(),
              onClick = { onIndexClick(index) }
          )
        }
      }

      val containerColor = when (selectionState) {
        NoSelection -> MaterialTheme.colorScheme.surface
        is ShowSelection ->
          if (index == selectionState.selectedWordIndex) {
            MaterialTheme.colorScheme.surfaceVariant
          } else {
            MaterialTheme.colorScheme.surface
          }
      }
      val borderStroke = when (selectionState) {
        NoSelection -> BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
        is ShowSelection ->
          if (index == selectionState.selectedWordIndex) {
            null
          } else {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
            )
          }
      }

      // TODO: Card selection overfills the Card
      Card(
          colors = CardDefaults.cardColors(containerColor = containerColor),
          border = borderStroke,
          modifier = Modifier
              .then(interactionModifier)
              .fillMaxWidth()
      ) {
        Text(
            text = quote.symbol,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
      }
    }
  }
}

/**
 * The content for the detail pane.
 */
@Composable
private fun DetailContent(
  quote: Quote?,
  modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier
          .verticalScroll(rememberScrollState())
          .padding(vertical = 16.dp)
  ) {
    if (quote != null) {
      Text(
          text = quote.symbol,
          style = MaterialTheme.typography.headlineMedium
      )
      Text(
          text = quote.name
      )
    } else Text(
        text = "Select a quote"
    )
  }
}


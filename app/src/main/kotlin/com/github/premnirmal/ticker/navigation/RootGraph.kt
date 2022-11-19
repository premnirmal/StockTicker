package com.github.premnirmal.ticker.navigation

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.detail.QuoteDetailScreen
import com.github.premnirmal.ticker.home.HomeListDetail
import com.github.premnirmal.ticker.network.data.Quote

@Composable
fun RootNavigationGraph(
  windowWidthSizeClass: WindowWidthSizeClass,
  windowHeightSizeClass: WindowHeightSizeClass,
  displayFeatures: List<DisplayFeature>,
  navHostController: NavHostController,
  onFragmentChange: (Int, Fragment) -> Unit
) {
  NavHost(
      navController = navHostController,
      route = Graph.ROOT,
      startDestination = Graph.HOME
  ) {
    composable(route = Graph.HOME) {
      HomeListDetail(
          rootNavController = navHostController,
          windowWidthSizeClass = windowWidthSizeClass,
          windowHeightSizeClass = windowHeightSizeClass, displayFeatures = displayFeatures,
          onFragmentChange = onFragmentChange
      )
    }
    composable(route = "${Graph.QUOTE_DETAIL}/{symbol}") {
      val symbol = it.arguments?.getString("symbol")
      symbol?.let { symbol ->
        QuoteDetailScreen(
            widthSizeClass = windowWidthSizeClass,
            contentType = null,
            displayFeatures = displayFeatures,
            quote = Quote(symbol = symbol)
        )
      }
    }
  }
}

object Graph {
  const val ROOT = "root_graph"
  const val HOME = "home_graph"
  const val QUOTE_DETAIL = "quote_detail_graph"
}
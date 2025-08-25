package com.github.premnirmal.ticker.navigation

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
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
    navHostController: NavHostController
) {
    val viewModelStoreOwner = rememberViewModelStoreOwner()
    CompositionLocalProvider(LocalNavGraphViewModelStoreOwner provides viewModelStoreOwner) {
        NavHost(
            navController = navHostController,
            route = Graph.ROOT,
            startDestination = Graph.HOME
        ) {
            composable(route = "${Graph.QUOTE_DETAIL}/{symbol}") {
                val symbol = it.arguments?.getString("symbol")
                symbol?.let { symbol ->
                    QuoteDetailScreen(
                        widthSizeClass = windowWidthSizeClass,
                        contentType = null,
                        displayFeatures = displayFeatures,
                        quote = Quote(symbol = symbol)
                    )
                    return@composable
                }
            }
            composable(route = Graph.HOME) {
                HomeListDetail(
                    rootNavController = navHostController,
                    windowWidthSizeClass = windowWidthSizeClass,
                    windowHeightSizeClass = windowHeightSizeClass,
                    displayFeatures = displayFeatures
                )
            }
        }
    }
}

@Composable
private fun rememberViewModelStoreOwner(): ViewModelStoreOwner {
    val context = LocalContext.current
    return remember(context) { context as ViewModelStoreOwner }
}

object Graph {
    const val ROOT = "root_graph"
    const val HOME = "home_graph"
    const val QUOTE_DETAIL = "quote_detail_graph"
}

val LocalNavGraphViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> {
    error("No LocalNavGraphViewModelStoreOwner provided")
}

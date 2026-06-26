package com.github.premnirmal.ticker.navigation

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.detail.QuoteDetailScreen
import com.github.premnirmal.ticker.home.HomeListDetail
import com.github.premnirmal.ticker.network.data.Quote

@Composable
fun RootNavigationGraphHost(
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    displayFeatures: List<DisplayFeature>,
    navHostController: NavHostController
) {
    val viewModelStoreOwner = rememberViewModelStoreOwner()
    CompositionLocalProvider(LocalNavGraphViewModelStoreOwner provides viewModelStoreOwner) {
        RootNavigationGraph(
            navHostController = navHostController,
            homeContent = {
                HomeListDetail(
                    rootNavController = navHostController,
                    windowWidthSizeClass = windowWidthSizeClass,
                    windowHeightSizeClass = windowHeightSizeClass,
                    displayFeatures = displayFeatures
                )
            },
            quoteDetailContent = { symbol ->
                QuoteDetailScreen(
                    widthSizeClass = windowWidthSizeClass,
                    contentType = null,
                    displayFeatures = displayFeatures,
                    quote = Quote(symbol = symbol)
                )
            }
        )
    }
}

@Composable
private fun rememberViewModelStoreOwner(): ViewModelStoreOwner {
    val context = LocalContext.current
    return remember(context) { context as ViewModelStoreOwner }
}

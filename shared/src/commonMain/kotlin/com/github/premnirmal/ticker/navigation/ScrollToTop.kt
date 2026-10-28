package com.github.premnirmal.ticker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Composition local carrying the [ViewModelStoreOwner] scoped to the root navigation graph, so the
 * shared [NavigationViewModel] (which drives the scroll-to-top actions) is shared across the home
 * destinations. The platform root graph provides it.
 */
val LocalNavGraphViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> {
    error("No LocalNavGraphViewModelStoreOwner provided")
}

/**
 * Registers a scroll-to-top action for the given [route] against the shared [NavigationViewModel]
 * and returns a [State] that flips to `true` while the [scrollToTop] action runs.
 */
@Composable
fun rememberScrollToTopAction(
    route: HomeRoute,
    data: Any? = null,
    scrollToTop: suspend () -> Unit
): State<Boolean> {
    val viewModelStoreOwner = checkNotNull(LocalNavGraphViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalNavGraphViewModelStoreOwner"
    }
    val navigationViewModel = viewModel<NavigationViewModel>(viewModelStoreOwner) {
        NavigationViewModel()
    }
    val scrolling = remember {
        mutableStateOf(false)
    }
    LaunchedEffect(route, data) {
        navigationViewModel.actionForRoute(route).collect {
            scrolling.value = true
            scrollToTop()
            scrolling.value = false
        }
    }
    return scrolling
}

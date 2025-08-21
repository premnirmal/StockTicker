package com.github.premnirmal.ticker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun rememberScrollToTopAction(
    route: HomeRoute,
    data: Any? = null,
    scrollToTop: suspend () -> Unit
): State<Boolean> {
    val viewModelStoreOwner = checkNotNull(LocalNavGraphViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalNavGraphViewModelStoreOwner"
    }
    val navigationViewModel = viewModel<NavigationViewModel>(viewModelStoreOwner)
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

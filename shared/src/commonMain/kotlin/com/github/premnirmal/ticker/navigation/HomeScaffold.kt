package com.github.premnirmal.ticker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.premnirmal.ticker.ui.NavigationContentPosition
import com.github.premnirmal.ticker.ui.NavigationType

/**
 * Multiplatform scaffold for the home screen. Chooses between bottom-navigation and rail layouts
 * depending on [navigationType]. All content (the nav host, bottom bar, and rail) is supplied via
 * composable slots or pre-built navigation components, keeping this free of Android resource
 * dependencies.
 */
@Composable
fun HomeScaffold(
    navigationType: NavigationType,
    selectedDestination: String,
    destinations: List<HomeBottomNavDestination>,
    navigationContentPosition: NavigationContentPosition,
    snackbarHostState: SnackbarHostState,
    navigateToTopLevelDestination: (HomeBottomNavDestination) -> Unit,
    navHost: @Composable (Modifier) -> Unit,
) {
    if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            bottomBar = {
                BottomNavigationBar(
                    selectedDestination = selectedDestination,
                    navigateToTopLevelDestination = navigateToTopLevelDestination,
                    destinations = destinations
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { padding ->
            navHost(Modifier.padding(bottom = padding.calculateBottomPadding()))
        }
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                HomeNavigationRail(
                    selectedDestination = selectedDestination,
                    navigationContentPosition = navigationContentPosition,
                    navigateToTopLevelDestination = navigateToTopLevelDestination,
                    destinations = destinations
                )
                navHost(Modifier.weight(1f))
            }
        }
    }
}

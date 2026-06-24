package com.github.premnirmal.ticker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
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
        // Capture the home content into a graphics layer so the glass bottom bar can draw a blurred
        // slice of whatever is scrolling behind it. The content is rendered edge-to-edge (the bar
        // floats over it) so it extends underneath the translucent bar, producing the glass look.
        val backdrop = rememberGraphicsLayer()
        // The floating bar overlays the home content, so it reports its measured height here and the
        // content exposes it through LocalContentBottomPadding so scrollable lists can pad their
        // bottom and let the last item scroll clear of the bar.
        var bottomBarHeight by remember { mutableStateOf(0.dp) }
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Each home screen supplies its own top-bar insets, so the nav host fills the whole
                // area (no extra top padding here, which previously double-padded the status bar).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            backdrop.record { this@drawWithContent.drawContent() }
                            drawLayer(backdrop)
                        }
                ) {
                    CompositionLocalProvider(LocalContentBottomPadding provides bottomBarHeight) {
                        navHost(Modifier.fillMaxSize())
                    }
                }
                BottomNavigationBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    selectedDestination = selectedDestination,
                    navigateToTopLevelDestination = navigateToTopLevelDestination,
                    destinations = destinations,
                    backdrop = backdrop,
                    onHeightChanged = { bottomBarHeight = it },
                )
            }
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

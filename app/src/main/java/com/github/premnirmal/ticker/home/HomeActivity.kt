package com.github.premnirmal.ticker.home

import android.Manifest
import android.os.Build.VERSION
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.rememberNavController
import com.github.premnirmal.ticker.base.BaseComposeActivity
import com.github.premnirmal.ticker.hasNotificationPermission
import com.github.premnirmal.ticker.navigation.Graph
import com.github.premnirmal.ticker.navigation.RootNavigationGraph
import com.github.premnirmal.tickerwidget.R
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : BaseComposeActivity() {
    override val simpleName = "HomeActivity"

    @Inject internal lateinit var appReviewManager: IAppReviewManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (VERSION.SDK_INT >= 33) {
            requestPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
                if (granted) {
                    viewModel.initNotifications()
                } else {
                    appMessaging.sendSnackbar(R.string.notification_alerts_required_message)
                }
            }
        }
        if (VERSION.SDK_INT >= 33 && appPreferences.notificationAlerts() && !hasNotificationPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Composable
    override fun ShowContent() {
        HomeScreen()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    private fun HomeScreen() {
        val windowSizeClass = calculateWindowSizeClass(this)
        val navHostController = rememberNavController()
        DisposableEffect(navHostController) {
            val listener = NavController.OnDestinationChangedListener { _: NavController, destination: NavDestination, _: Bundle? ->
                if (destination.route == Graph.QUOTE_DETAIL) {
                    viewModel.sendHomeEvent(HomeEvent.PromptRate)
                }
            }
            navHostController.addOnDestinationChangedListener(listener)
            onDispose {
                navHostController.removeOnDestinationChangedListener(listener)
            }
        }
        val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }
        var rateDialogShown by rememberSaveable {
            mutableStateOf(false)
        }
        RootNavigationGraph(
            windowWidthSizeClass = windowSizeClass.widthSizeClass,
            windowHeightSizeClass = windowSizeClass.heightSizeClass,
            displayFeatures = calculateDisplayFeatures(this),
            navHostController = navHostController
        )
        LaunchedEffect(Unit) {
            intent.getStringExtra(EXTRA_SYMBOL)?.let {
                navHostController.navigate(route = "${Graph.QUOTE_DETAIL}/$it")
            }
        }
        LaunchedEffect(appPreferences.getLastSavedVersionCode(), appPreferences.tutorialShown()) {
            delay(1000L) // delay to ensure splash screen is shown
            viewModel.checkShowWhatsNew()
            viewModel.checkShowTutorial()
        }
        LaunchedEffect(Unit) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.homeEvent.collect { event ->
                    when (event) {
                        is HomeEvent.PromptRate -> {
                            if (!rateDialogShown && appPreferences.shouldPromptRate()) {
                                appReviewManager.launchReviewFlow(this@HomeActivity)
                                rateDialogShown = true
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(Unit) {
            viewModel.fetchPortfolioInRealTime()
        }
    }

    companion object {
        const val EXTRA_SYMBOL: String = "EXTRA_SYMBOL"
    }
}

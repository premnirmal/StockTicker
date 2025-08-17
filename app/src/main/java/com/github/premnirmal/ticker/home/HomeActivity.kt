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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.github.premnirmal.ticker.base.BaseComposeActivity
import com.github.premnirmal.ticker.hasNotificationPermission
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.navigation.Graph
import com.github.premnirmal.ticker.navigation.RootNavigationGraph
import com.github.premnirmal.ticker.showBottomSheet
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : BaseComposeActivity() {
    override val simpleName = "HomeActivity"

    @Inject internal lateinit var appReviewManager: IAppReviewManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val viewModel: HomeViewModel by viewModels()
    private var rateDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { rateDialogShown = it.getBoolean(DIALOG_SHOWN, false) }
        if (appPreferences.getLastSavedVersionCode() < BuildConfig.VERSION_CODE) {
            viewModel.showWhatsNew()
        } else {
            val tutorialShown = appPreferences.tutorialShown()
            if (!tutorialShown) {
                showTutorial()
            }
        }
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.homeEvent.collect { event ->
                    when (event) {
                        is HomeEvent.ShowTutorial -> {
                            showTutorial()
                        }
                        is HomeEvent.ShowWhatsNew -> {
                            showWhatsNew(event.result)
                        }
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
        viewModel.fetchPortfolioInRealTime()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(DIALOG_SHOWN, rateDialogShown)
        super.onSaveInstanceState(outState)
    }

    @Composable
    override fun ShowContent() {
        HomeScreen()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    fun HomeScreen() {
        val windowSizeClass = calculateWindowSizeClass(this)
        val navHostController = rememberNavController()
        RootNavigationGraph(
            windowWidthSizeClass = windowSizeClass.widthSizeClass,
            windowHeightSizeClass = windowSizeClass.heightSizeClass,
            displayFeatures = calculateDisplayFeatures(this),
            navHostController = navHostController
        )
        intent.getStringExtra(EXTRA_SYMBOL)?.let {
            navHostController.navigate(route = "${Graph.QUOTE_DETAIL}/$it")
        }
    }

    private fun showTutorial() {
        showBottomSheet(getString(R.string.how_to_title), getString(R.string.how_to))
        appPreferences.setTutorialShown(true)
    }

    private fun showWhatsNew(result: FetchResult<List<String>>) {
        val message = with(result) {
            if (wasSuccessful) {
                appPreferences.saveVersionCode(BuildConfig.VERSION_CODE)
                data.joinToString("\n\u25CF ", "\u25CF ")
            } else {
                "${getString(R.string.error_fetching_whats_new)}\n\n :( ${error.message.orEmpty()}"
            }
        }
        showBottomSheet(
            getString(R.string.whats_new_in, BuildConfig.VERSION_NAME),
            message
        )
    }

    companion object {
        private const val DIALOG_SHOWN: String = "DIALOG_SHOWN"
        const val EXTRA_SYMBOL: String = "EXTRA_SYMBOL"
    }
}

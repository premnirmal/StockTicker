package com.github.premnirmal.ticker.home

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.github.premnirmal.ticker.base.BaseComposeActivity
import com.github.premnirmal.ticker.navigation.RootNavigationGraph
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : BaseComposeActivity() {
  override val simpleName = "HomeActivity"

  val viewModel: HomeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
  }

  @Composable
  override fun ShowContent() {
    HomeScreen()
  }

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  @Composable
  fun HomeScreen() {
    val windowSizeClass = calculateWindowSizeClass(this)
    RootNavigationGraph(
        windowWidthSizeClass = windowSizeClass.widthSizeClass,
        windowHeightSizeClass = windowSizeClass.heightSizeClass,
        displayFeatures = calculateDisplayFeatures(this),
        navHostController = rememberNavController()
    )
  }
}
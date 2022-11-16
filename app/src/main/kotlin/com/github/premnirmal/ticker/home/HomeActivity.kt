package com.github.premnirmal.ticker.home

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import com.github.premnirmal.ticker.base.BaseComposeActivity
import com.github.premnirmal.tickerwidget.ui.theme.AppTheme
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : BaseComposeActivity() {
  override val simpleName = "MainActivity2"

  val viewModel: HomeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme(theme = currentTheme) {
        HomeScreen()
      }
    }
  }

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  @Composable
  fun HomeScreen() {
    val windowSizeClass = calculateWindowSizeClass(this)
    HomeListDetail(
        windowWidthSizeClass = windowSizeClass.widthSizeClass,
        windowHeightSizeClass = windowSizeClass.heightSizeClass,
        displayFeatures = calculateDisplayFeatures(this)
    )
  }
}
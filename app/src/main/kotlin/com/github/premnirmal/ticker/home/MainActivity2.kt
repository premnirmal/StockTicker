package com.github.premnirmal.ticker.home

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import com.github.premnirmal.ticker.base.BaseComposeActivity
import com.github.premnirmal.tickerwidget.ui.theme.AppTheme

class MainActivity2 : BaseComposeActivity() {
  override val simpleName = "MainActivity2"

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme(currentTheme) {
        val windowSize = calculateWindowSizeClass(this)
        HomeScreen(windowSize.widthSizeClass)
      }
    }
  }

  @Composable
  fun HomeScreen(windowSize: WindowWidthSizeClass) {
    if (windowSize == WindowWidthSizeClass.Expanded) {

    } else {

    }
  }
}
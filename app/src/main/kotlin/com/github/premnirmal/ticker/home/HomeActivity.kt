package com.github.premnirmal.ticker.home

import android.os.Bundle
import android.util.SparseArray
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.Fragment.SavedState
import androidx.fragment.app.FragmentManager
import androidx.navigation.compose.rememberNavController
import com.github.premnirmal.ticker.base.BaseComposeActivityForFragments
import com.github.premnirmal.ticker.navigation.RootNavigationGraph
import com.github.premnirmal.tickerwidget.ui.theme.AppTheme
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : BaseComposeActivityForFragments() {
  override val simpleName = "HomeActivity"

  val viewModel: HomeViewModel by viewModels()

  var savedStateSparseArray = SparseArray<SavedState>()
  private var currentSelectItemId = -1

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putSparseParcelableArray(
        SAVED_STATE_CONTAINER_KEY, savedStateSparseArray)
    outState.putInt(
        SAVED_STATE_CURRENT_TAB_KEY, currentSelectItemId)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      savedStateSparseArray = savedInstanceState
          .getSparseParcelableArray(
              SAVED_STATE_CONTAINER_KEY
          )
          ?: savedStateSparseArray
      currentSelectItemId = savedInstanceState.getInt(SAVED_STATE_CURRENT_TAB_KEY)
    }
    setContent {
      AppTheme(theme = currentTheme) {
        ApplyThemeColourToNavigationBar()
        ApplyThemeColourToStatusBar()
        HomeScreen()
      }
    }
  }

  private fun saveAndRetrieveFragment(
    supportFragmentManager: FragmentManager,
    tabId: Int,
    fragment: Fragment
  ) {
    val currentFragment = supportFragmentManager
        .findFragmentById(currentSelectItemId)
    if (currentFragment != null) {
      savedStateSparseArray.put(
          currentSelectItemId,
          supportFragmentManager
              .saveFragmentInstanceState(currentFragment)
      )
    }
    currentSelectItemId = tabId
    fragment.setInitialSavedState(
        savedStateSparseArray[currentSelectItemId])
  }

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  @Composable
  fun HomeScreen() {
    val windowSizeClass = calculateWindowSizeClass(this)
    RootNavigationGraph(
        windowWidthSizeClass = windowSizeClass.widthSizeClass,
        windowHeightSizeClass = windowSizeClass.heightSizeClass,
        displayFeatures = calculateDisplayFeatures(this),
        navHostController = rememberNavController(),
        onFragmentChange = { tabId, fragment ->
          saveAndRetrieveFragment(supportFragmentManager, tabId, fragment)
        }
    )
  }

  companion object {
    private const val SAVED_STATE_CONTAINER_KEY = "SAVED_STATE_CONTAINER_KEY"
    private const val SAVED_STATE_CURRENT_TAB_KEY = "SAVED_STATE_CURRENT_TAB_KEY"
  }
}
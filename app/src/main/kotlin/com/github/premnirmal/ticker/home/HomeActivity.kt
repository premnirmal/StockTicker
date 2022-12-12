package com.github.premnirmal.ticker.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.github.premnirmal.ticker.base.BaseComposeActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.hasNotificationPermission
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.navigation.RootNavigationGraph
import com.github.premnirmal.ticker.settings.PortfolioExporter
import com.github.premnirmal.ticker.settings.TickersExporter
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
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
    val tutorialShown = appPreferences.tutorialShown()
    if (!tutorialShown) {
      showTutorial()
    }
    if (appPreferences.getLastSavedVersionCode() < BuildConfig.VERSION_CODE) {
      viewModel.showWhatsNew()
    }
    if (VERSION.SDK_INT >= 33) {
      requestPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
        if (granted) {
          viewModel.initNotifications()
        } else {
          InAppMessage.showMessage(this, R.string.notification_alerts_required_message)
        }
      }
      viewModel.requestNotificationPermission.observe(this) {
        if (it == true) {
          requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          viewModel.resetRequestNotificationPermission()
        }
      }
    }
    if (VERSION.SDK_INT >= 33 && appPreferences.notificationAlerts() && !hasNotificationPermission()) {
      requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    viewModel.showTutorial.observe(this) {
      if (it == true) {
        showTutorial()
        viewModel.resetShowTutorial()
      }
    }
    viewModel.showWhatsNew.observe(this) { result ->
       result?.let {
        showWhatsNew(it)
        viewModel.resetShowWhatsNew()
      }
    }
    viewModel.promptRate.observe(this) {
      if (it && !rateDialogShown && !appPreferences.shouldPromptRate()) {
        appReviewManager.launchReviewFlow(this)
        rateDialogShown = true
      }
    }
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
    RootNavigationGraph(
        windowWidthSizeClass = windowSizeClass.widthSizeClass,
        windowHeightSizeClass = windowSizeClass.heightSizeClass,
        displayFeatures = calculateDisplayFeatures(this),
        navHostController = rememberNavController()
    )
  }

  private fun showTutorial() {
    showDialog(getString(R.string.how_to_title), getString(R.string.how_to))
    appPreferences.setTutorialShown(true)
  }

  private fun showWhatsNew(result: FetchResult<List<String>>) {
    val dialog = showDialog(
        getString(R.string.whats_new_in, BuildConfig.VERSION_NAME),
        getString(R.string.loading)
    )
    result.apply {
      if (wasSuccessful) {
        val whatsNew = data.joinToString("\n\u25CF ", "\u25CF ")
        dialog.setMessage(whatsNew)
        appPreferences.saveVersionCode(BuildConfig.VERSION_CODE)
      } else {
        dialog.setMessage(
            "${getString(R.string.error_fetching_whats_new)}\n\n :( ${error.message.orEmpty()}"
        )
      }
    }
  }

  private fun needsPermissionGrant(): Boolean {
    if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return false
    }
    return VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) != PackageManager.PERMISSION_GRANTED
  }

  private fun askForExternalStoragePermissions(reqCode: Int) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ), reqCode
    )
  }

  private fun exportAndShareTickers(uri: Uri) {
    lifecycleScope.launch {
      val result = TickersExporter.exportTickers(this@HomeActivity, uri, stocksProvider.tickers.value)
      if (result == null) {
        showDialog(getString(R.string.error_sharing))
        Timber.w(Throwable("Error sharing tickers"))
      } else {
        shareTickers(uri)
      }
    }
  }

  private fun launchExportIntent() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "application/json"
    }
//    startActivityForResult(intent, REQCODE_FILE_WRITE)
  }

  private fun launchExportForShareIntent() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "text/plain"
    }
//    startActivityForResult(intent, REQCODE_FILE_WRITE_SHARE)
  }

  private fun launchImportIntent() {
    val intent =
      Intent(Intent.ACTION_OPEN_DOCUMENT)
    intent.type = "*/*"
//    startActivityForResult(intent, REQCODE_FILE_READ)
  }

  private fun exportPortfolio(uri: Uri) {
    lifecycleScope.launch {
      val result = PortfolioExporter.exportQuotes(this@HomeActivity, uri, stocksProvider.portfolio.value)
      if (result == null) {
        showDialog(getString(R.string.error_exporting))
        Timber.w(Throwable("Error exporting tickers"))
      } else {
        showDialog(getString(R.string.exported_to))
      }
    }
  }

  private fun shareTickers(uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>())
    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.my_stock_portfolio))
    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_email_subject))
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    val launchIntent = Intent.createChooser(intent, getString(R.string.action_share))
    if (VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      launchIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(launchIntent)
  }

  companion object {
    private const val DIALOG_SHOWN: String = "DIALOG_SHOWN"
  }
}
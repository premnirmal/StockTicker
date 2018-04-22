package com.github.premnirmal.ticker.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity.Companion.getStatusBarHeight
import com.github.premnirmal.ticker.base.BaseActivity.Companion.showDialog
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import kotlinx.android.synthetic.main.activity_preferences.toolbar
import kotlinx.android.synthetic.main.preferences_footer.version
import timber.log.Timber
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class SettingsActivity : PreferenceActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

  companion object {
    private val REQCODE_WRITE_EXTERNAL_STORAGE = 850
    private val REQCODE_READ_EXTERNAL_STORAGE = 851
    private val REQCODE_WRITE_EXTERNAL_STORAGE_SHARE = 852
    private val REQCODE_FILE_WRITE = 853
  }

  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var preferences: SharedPreferences
  @Inject internal lateinit var appPreferences: AppPreferences

  override fun onPause() {
    super.onPause()
    broadcastUpdateWidget()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
    setContentView(R.layout.activity_preferences)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      toolbar.setPadding(toolbar.paddingLeft, getStatusBarHeight(),
          toolbar.paddingRight, toolbar.paddingBottom)
    }
  }

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    val bar = toolbar
    bar.setNavigationOnClickListener({ onBackPressed() })

    listView.addFooterView(
        LayoutInflater.from(this).inflate(R.layout.preferences_footer, null, false))

    val versionView = version
    val vName = "v${BuildConfig.VERSION_NAME}"
    versionView.text = vName
    setupSimplePreferencesScreen()
  }

  /**
   * Shows the simplified settings UI if the device configuration if the
   * device configuration dictates that a simplified, single-pane UI should be
   * shown.
   */
  @SuppressLint("CommitPrefEdits")
  private fun setupSimplePreferencesScreen() {
    // In the simplified UI, fragments are not used at all and we instead
    // use the older PreferenceActivity APIs.

    // Add 'general' preferences.
    addPreferencesFromResource(R.xml.prefs)

    run({
      val nukePref = findPreference(AppPreferences.SETTING_NUKE)
      nukePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        showDialog(getString(R.string.are_you_sure), OnClickListener { _, _ ->
          val hasUserAlreadyRated = appPreferences.hasUserAlreadyRated()
          Timber.w(RuntimeException("Nuked from settings!"))
          preferences.edit().clear().commit()
          val directory = filesDir.path + "$packageName/shared_prefs/"
          val sharedPreferenceFile = File(directory)
          val listFiles = sharedPreferenceFile.listFiles()
          listFiles?.forEach { file ->
            file?.delete()
          }
          preferences.edit().putBoolean(AppPreferences.DID_RATE, hasUserAlreadyRated).commit()
          System.exit(0)
        })
        true
      }
    })

    run({
      val exportPref = findPreference(AppPreferences.SETTING_EXPORT)
      exportPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        if (needsPermissionGrant()) {
          askForExternalStoragePermissions(REQCODE_WRITE_EXTERNAL_STORAGE)
        } else {
          exportTickers()
        }
        true
      }
    })

    run({
      val sharePref = findPreference(AppPreferences.SETTING_SHARE)
      sharePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        if (needsPermissionGrant()) {
          askForExternalStoragePermissions(REQCODE_WRITE_EXTERNAL_STORAGE_SHARE)
        } else {
          exportAndShareTickers()
        }
        true
      }
    })

    run({
      val importPref = findPreference(AppPreferences.SETTING_IMPORT)
      importPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        if (needsPermissionGrant()) {
          askForExternalStoragePermissions(REQCODE_READ_EXTERNAL_STORAGE)
        } else {
          launchImportIntent()
        }
        true
      }
    })

    run({
      val fontSizePreference = findPreference(AppPreferences.FONT_SIZE) as ListPreference
      val size = preferences.getInt(AppPreferences.FONT_SIZE, 1)
      fontSizePreference.setValueIndex(size)
      fontSizePreference.summary = fontSizePreference.entries[size]
      fontSizePreference.onPreferenceChangeListener = object : DefaultPreferenceChangeListener() {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
          val stringValue = newValue.toString()
          val listPreference = preference as ListPreference
          val index = listPreference.findIndexOfValue(stringValue)
          preferences.edit().remove(AppPreferences.FONT_SIZE).putInt(AppPreferences.FONT_SIZE,
              index).apply()
          broadcastUpdateWidget()
          fontSizePreference.summary = fontSizePreference.entries[index]
          InAppMessage.showMessage(this@SettingsActivity, R.string.text_size_updated_message)
          return true
        }
      }
    })

    run({
      val refreshPreference = findPreference(AppPreferences.UPDATE_INTERVAL) as ListPreference
      val refreshIndex = preferences.getInt(AppPreferences.UPDATE_INTERVAL, 1)
      refreshPreference.setValueIndex(refreshIndex)
      refreshPreference.summary = refreshPreference.entries[refreshIndex]
      refreshPreference.onPreferenceChangeListener = object : DefaultPreferenceChangeListener() {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
          val stringValue = newValue.toString()
          val listPreference = preference as ListPreference
          val index = listPreference.findIndexOfValue(stringValue)
          preferences.edit().putInt(AppPreferences.UPDATE_INTERVAL, index).apply()
          broadcastUpdateWidget()
          refreshPreference.summary = refreshPreference.entries[index]
          InAppMessage.showMessage(this@SettingsActivity, R.string.refresh_updated_message)
          return true
        }
      }
    })

    run({
      val startTimePref = findPreference(AppPreferences.START_TIME) as TimePreference
      startTimePref.summary = preferences.getString(AppPreferences.START_TIME, "09:30")
      startTimePref.onPreferenceChangeListener = object : DefaultPreferenceChangeListener() {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
          val startTimez = appPreferences.timeAsIntArray(newValue.toString())
          val endTimez = appPreferences.endTime()
          if (endTimez[0] == startTimez[0] && endTimez[1] == startTimez[1]) {
            showDialog(getString(R.string.incorrect_time_update_error))
            return false
          } else {
            preferences.edit().putString(AppPreferences.START_TIME, newValue.toString()).apply()
            startTimePref.summary = newValue.toString()
            stocksProvider.schedule()
            InAppMessage.showMessage(this@SettingsActivity, R.string.start_time_updated)
            return true
          }
        }
      }
    })

    run({
      val endTimePref = findPreference(AppPreferences.END_TIME) as TimePreference
      endTimePref.summary = preferences.getString(AppPreferences.END_TIME, "16:30")
      run({
        val endTimez = appPreferences.endTime()
        val startTimez = appPreferences.startTime()
        if (endTimez[0] == startTimez[0] && endTimez[1] == startTimez[1]) {
          endTimePref.setSummary(R.string.incorrect_time_update_error)
        }
      })
      endTimePref.onPreferenceChangeListener = object : DefaultPreferenceChangeListener() {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
          val endTimez = appPreferences.timeAsIntArray(newValue.toString())
          val startTimez = appPreferences.startTime()
          if (endTimez[0] == startTimez[0] && endTimez[1] == startTimez[1]) {
            showDialog(getString(R.string.incorrect_time_update_error))
            return false
          } else {
            preferences.edit().putString(AppPreferences.END_TIME, newValue.toString()).apply()
            endTimePref.summary = newValue.toString()
            stocksProvider.schedule()
            InAppMessage.showMessage(this@SettingsActivity, R.string.end_time_updated)
            return true
          }
        }
      }
    })
  }

  private fun needsPermissionGrant(): Boolean {
    return Build.VERSION.SDK_INT >= 23 &&
        ContextCompat.checkSelfPermission(this@SettingsActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
  }

  private fun askForExternalStoragePermissions(reqCode: Int) {
    ActivityCompat.requestPermissions(this@SettingsActivity,
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE),
        reqCode)
  }

  private fun exportAndShareTickers() {
    val file = AppPreferences.tickersFile
    if (file.exists()) {
      shareTickers()
    } else {
      object : FileExportTask() {
        override fun onPostExecute(result: String?) {
          if (result == null) {
            showDialog(getString(R.string.error_sharing))
            Timber.w(Throwable("Error sharing tickers"))
          } else {
            shareTickers()
          }
        }
      }.execute(stocksProvider.getTickers())
    }
  }

  private fun launchImportIntent() {
    MaterialFilePicker()
        .withActivity(this)
        .withRequestCode(REQCODE_FILE_WRITE)
        .withFilter(Pattern.compile(
            ".*\\.txt$")) // Filtering files and directories by file name using regexp
        .start()
  }

  private fun exportTickers() {
    object : FileExportTask() {
      override fun onPostExecute(result: String?) {
        if (result == null) {
          showDialog(getString(R.string.error_exporting))
          Timber.w(Throwable("Error exporting tickers"))
        } else {
          showDialog(getString(R.string.exported_to, result))
        }
      }
    }.execute(stocksProvider.getTickers())
  }

  private fun shareTickers() {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>())
    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.my_stock_portfolio))
    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_email_subject))
    val file = AppPreferences.tickersFile
    if (!file.exists() || !file.canRead()) {
      showDialog(getString(R.string.error_sharing))
      Timber.w(Throwable("Error sharing tickers"))
      return
    }
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
    } else {
      Uri.fromFile(file)
    }
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    val launchIntent = Intent.createChooser(intent, getString(R.string.action_share))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      launchIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(launchIntent)
  }

  private fun broadcastUpdateWidget() {
    widgetDataProvider.broadcastUpdateAllWidgets()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
      grantResults: IntArray) {
    when (requestCode) {
      REQCODE_WRITE_EXTERNAL_STORAGE -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          exportTickers()
        } else {
          showDialog(getString(R.string.cannot_export_msg))
        }
      }
      REQCODE_READ_EXTERNAL_STORAGE -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          launchImportIntent()
        } else {
          showDialog(getString(R.string.cannot_import_msg))
        }
      }
      REQCODE_WRITE_EXTERNAL_STORAGE_SHARE -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          exportAndShareTickers()
        } else {
          showDialog(getString(R.string.cannot_share_msg))
        }
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQCODE_FILE_WRITE && resultCode == Activity.RESULT_OK) {
      val filePath = data?.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)
      if (filePath != null) {
        object : FileImportTask(widgetDataProvider) {
          override fun onPostExecute(result: Boolean?) {
            if (result != null && result) {
              showDialog(getString(R.string.ticker_import_success))
            } else {
              showDialog(getString(R.string.ticker_import_fail))
            }
          }
        }.execute(filePath)
      }
    }
    super.onActivityResult(requestCode, resultCode, data)
  }
}
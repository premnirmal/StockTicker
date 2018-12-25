package com.github.premnirmal.ticker.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TimePicker
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.toBitmap
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import saschpe.android.customtabs.CustomTabsHelper
import saschpe.android.customtabs.WebViewFallback
import timber.log.Timber
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Created by premnirmal on 2/27/16.
 */
class SettingsFragment : PreferenceFragmentCompat(),
    ActivityCompat.OnRequestPermissionsResultCallback {

  companion object {
    private const val REQCODE_WRITE_EXTERNAL_STORAGE = 850
    private const val REQCODE_READ_EXTERNAL_STORAGE = 851
    private const val REQCODE_WRITE_EXTERNAL_STORAGE_SHARE = 852
    private const val REQCODE_FILE_WRITE = 853
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
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupSimplePreferencesScreen()
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.prefs, rootKey)
  }

  override fun onDisplayPreferenceDialog(preference: Preference) {
    if (preference.key == AppPreferences.START_TIME) {
      val pref = preference as TimePreference
      val dialog = TimePickerDialog(
          context, TimeSelectedListener(pref, R.string.start_time_updated), pref.lastHour,
          pref.lastMinute, true
      )
      dialog.setTitle(R.string.start_time)
      dialog.show()
    } else if (preference.key == AppPreferences.END_TIME) {
      val pref = preference as TimePreference
      val dialog = TimePickerDialog(
          context, TimeSelectedListener(pref, R.string.end_time_updated), pref.lastHour,
          pref.lastMinute, true
      )
      dialog.setTitle(R.string.end_time)
      dialog.show()
    } else {
      super.onDisplayPreferenceDialog(preference)
    }
  }

  /**
   * Shows the simplified settings UI if the device configuration if the
   * device configuration dictates that a simplified, single-pane UI should be
   * shown.
   */
  @SuppressLint("CommitPrefEdits") private fun setupSimplePreferencesScreen() {
    run {
      val privacyPref = findPreference<Preference>(AppPreferences.SETTING_PRIVACY_POLICY)
      privacyPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        val customTabsIntent = CustomTabsIntent.Builder().addDefaultShareMenuItem()
            .setToolbarColor(this.resources.getColor(R.color.colorPrimary)).setShowTitle(true)
            .setCloseButtonIcon(resources.getDrawable(R.drawable.ic_close).toBitmap()).build()
        CustomTabsHelper.addKeepAliveExtra(context!!, customTabsIntent.intent)
        CustomTabsHelper.openCustomTab(
            context!!, customTabsIntent,
            Uri.parse(resources.getString(R.string.privacy_policy_url)), WebViewFallback()
        )
        true
      }
    }

    run {
      val nukePref = findPreference<Preference>(AppPreferences.SETTING_NUKE)
      nukePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        showDialog(getString(R.string.are_you_sure), DialogInterface.OnClickListener { _, _ ->
          val hasUserAlreadyRated = appPreferences.hasUserAlreadyRated()
          Timber.w(RuntimeException("Nuked from settings!"))
          preferences.edit().clear().commit()
          val packageName = context!!.packageName
          val filesDir = context!!.filesDir
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
    }

    run {
      val exportPref = findPreference<Preference>(AppPreferences.SETTING_EXPORT)
      exportPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        if (needsPermissionGrant()) {
          askForExternalStoragePermissions(REQCODE_WRITE_EXTERNAL_STORAGE)
        } else {
          exportTickers()
        }
        true
      }
    }

    run {
      val sharePref = findPreference<Preference>(AppPreferences.SETTING_SHARE)
      sharePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        if (needsPermissionGrant()) {
          askForExternalStoragePermissions(REQCODE_WRITE_EXTERNAL_STORAGE_SHARE)
        } else {
          exportAndShareTickers()
        }
        true
      }
    }

    run {
      val importPref = findPreference<Preference>(AppPreferences.SETTING_IMPORT)
      importPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        if (needsPermissionGrant()) {
          askForExternalStoragePermissions(REQCODE_READ_EXTERNAL_STORAGE)
        } else {
          launchImportIntent()
        }
        true
      }
    }

    run {
      val fontSizePreference = findPreference(AppPreferences.FONT_SIZE) as ListPreference
      val size = preferences.getInt(AppPreferences.FONT_SIZE, 1)
      fontSizePreference.setValueIndex(size)
      fontSizePreference.summary = fontSizePreference.entries[size]
      fontSizePreference.onPreferenceChangeListener = object : DefaultPreferenceChangeListener() {
        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
          val stringValue = newValue.toString()
          val listPreference = preference as ListPreference
          val index = listPreference.findIndexOfValue(stringValue)
          preferences.edit().remove(AppPreferences.FONT_SIZE).putInt(
              AppPreferences.FONT_SIZE, index
          ).apply()
          broadcastUpdateWidget()
          fontSizePreference.summary = fontSizePreference.entries[index]
          InAppMessage.showMessage(activity!!, R.string.text_size_updated_message)
          return true
        }
      }
    }

    run {
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
          InAppMessage.showMessage(activity!!, R.string.refresh_updated_message)
          return true
        }
      }
    }

    run {
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
            InAppMessage.showMessage(activity!!, R.string.start_time_updated)
            return true
          }
        }
      }
    }

    run {
      val endTimePref = findPreference(AppPreferences.END_TIME) as TimePreference
      endTimePref.summary = preferences.getString(AppPreferences.END_TIME, "16:30")
      run {
        val endTimez = appPreferences.endTime()
        val startTimez = appPreferences.startTime()
        if (endTimez[0] == startTimez[0] && endTimez[1] == startTimez[1]) {
          endTimePref.setSummary(R.string.incorrect_time_update_error)
        }
      }
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
            InAppMessage.showMessage(activity!!, R.string.end_time_updated)
            return true
          }
        }
      }
    }
  }

  private fun needsPermissionGrant(): Boolean {
    return Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(
        activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) != PackageManager.PERMISSION_GRANTED
  }

  private fun askForExternalStoragePermissions(reqCode: Int) {
    ActivityCompat.requestPermissions(
        activity!!, arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    ), reqCode
    )
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
    MaterialFilePicker().withSupportFragment(this).withRequestCode(REQCODE_FILE_WRITE)
        // Filtering files and directories by file name using regexp
        .withFilter(Pattern.compile(".*\\.txt$")).start()
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
      FileProvider.getUriForFile(context!!, BuildConfig.APPLICATION_ID + ".provider", file)
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

  inner class TimeSelectedListener(private val preference: TimePreference,
    private val messageRes: Int) : TimePickerDialog.OnTimeSetListener {

    override fun onTimeSet(picker: TimePicker, hourOfDay: Int, minute: Int) {
      val lastHour = picker.currentHour
      val lastMinute = picker.currentMinute
      val hourString = if (lastHour < 10) "0$lastHour" else lastHour.toString()
      val minuteString = if (lastMinute < 10) "0$lastMinute" else lastMinute.toString()
      val time = "$hourString:$minuteString"
      val startTimez = appPreferences.timeAsIntArray(time)
      val endTimez = appPreferences.endTime()
      if (endTimez[0] == startTimez[0] && endTimez[1] == startTimez[1]) {
        showDialog(getString(R.string.incorrect_time_update_error))
      } else {
        preferences.edit().putString(preference.key, time).apply()
        preference.summary = time
        stocksProvider.schedule()
        InAppMessage.showMessage(activity!!, messageRes)
      }
    }
  }
}
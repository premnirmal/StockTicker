package com.github.premnirmal.ticker.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.EditText
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.repo.QuotesDB
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.fragment_settings.toolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.format.TextStyle.SHORT
import timber.log.Timber
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * Created by premnirmal on 2/27/16.
 */
class SettingsFragment : PreferenceFragmentCompat(), ChildFragment,
    ActivityCompat.OnRequestPermissionsResultCallback {

  companion object {
    private const val REQCODE_WRITE_EXTERNAL_STORAGE = 850
    private const val REQCODE_READ_EXTERNAL_STORAGE = 851
    private const val REQCODE_WRITE_EXTERNAL_STORAGE_SHARE = 852
    private const val REQCODE_FILE_READ = 853
    private const val REQCODE_FILE_WRITE = 854
    private const val REQCODE_FILE_WRITE_SHARE = 855
  }

  interface Parent {
    fun showWhatsNew()
    fun showTutorial()
  }

  private val parent: Parent
    get() = context as Parent
  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var preferences: SharedPreferences
  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var db: QuotesDB

  // ChildFragment

  override fun setData(bundle: Bundle) {

  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    (toolbar.layoutParams as MarginLayoutParams).topMargin = requireContext().getStatusBarHeight()
    listView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    listView.isVerticalScrollBarEnabled = false
    setupSimplePreferencesScreen()
  }

  override fun onPause() {
    super.onPause()
    broadcastUpdateWidget()
  }

  override fun onCreatePreferences(
    savedInstanceState: Bundle?,
    rootKey: String?
  ) {
    setPreferencesFromResource(R.xml.prefs, rootKey)
  }

  override fun onDisplayPreferenceDialog(preference: Preference) {
    when {
      preference.key == AppPreferences.START_TIME || preference.key == AppPreferences.END_TIME-> {
        val pref = preference as TimePreference
        val dialog = createTimePickerDialog(pref)
        dialog.show()
      }
      else -> super.onDisplayPreferenceDialog(preference)
    }
  }

  /**
   * Shows the simplified settings UI if the device configuration if the
   * device configuration dictates that a simplified, single-pane UI should be
   * shown.
   */
  @SuppressLint("CommitPrefEdits") private fun setupSimplePreferencesScreen() {
    run {
      val pref = findPreference<Preference>(AppPreferences.SETTING_WHATS_NEW)
      pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        parent.showWhatsNew()
        true
      }
    }

    run {
      val pref = findPreference<Preference>(AppPreferences.SETTING_TUTORIAL)
      pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        parent.showTutorial()
        true
      }
    }

    run {
      val privacyPref = findPreference<Preference>(AppPreferences.SETTING_PRIVACY_POLICY)
      privacyPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        CustomTabs.openTab(
            requireContext(), resources.getString(R.string.privacy_policy_url)
        )
        true
      }
    }

    run {
      val themePref = findPreference<ListPreference>(AppPreferences.SETTING_APP_THEME)
      val selectedPref = appPreferences.themePref
      themePref.setValueIndex(selectedPref)
      themePref.summary = themePref.entries[selectedPref]
      themePref.onPreferenceChangeListener = object : DefaultPreferenceChangeListener() {
        override fun onPreferenceChange(
            preference: Preference,
            newValue: Any
        ): Boolean {
          val stringValue = newValue.toString()
          val listPreference = preference as ListPreference
          val index = listPreference.findIndexOfValue(stringValue)
          appPreferences.themePref = index
          themePref.summary = listPreference.entries[index]
          AppCompatDelegate.setDefaultNightMode(appPreferences.nightMode)
          InAppMessage.showMessage(requireActivity(), R.string.theme_updated_message)
          return true
        }
      }
    }

    run {
      val autoSortPref = findPreference<CheckBoxPreference>(AppPreferences.SETTING_AUTOSORT)
      val widgetData =
        widgetDataProvider.dataForWidgetId(AppWidgetManager.INVALID_APPWIDGET_ID)
      autoSortPref.isEnabled = !widgetDataProvider.hasWidget()
      autoSortPref.isChecked = widgetData.autoSortEnabled()
      autoSortPref.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { _, newValue ->
          widgetData.setAutoSort(newValue as Boolean)
          true
        }
    }

    run {
      val nukePref = findPreference<Preference>(AppPreferences.SETTING_NUKE)
      nukePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        showDialog(getString(R.string.are_you_sure), DialogInterface.OnClickListener { _, _ ->
          val hasUserAlreadyRated = appPreferences.hasUserAlreadyRated()
          Timber.w(RuntimeException("Nuked from settings!"))
          preferences.edit()
              .clear()
              .apply()
          preferences.edit()
              .putBoolean(AppPreferences.DID_RATE, hasUserAlreadyRated)
              .apply()
          lifecycleScope.launch(Dispatchers.IO) {
            db.clearAllTables()
            exitProcess(0)
          }
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
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            launchExportIntent()
          } else {
            exportPortfolio(AppPreferences.portfolioFile)
          }
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
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            launchExportForShareIntent()
          } else {
            exportAndShareTickers(AppPreferences.tickersFile)
          }
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
        override fun onPreferenceChange(
          preference: Preference,
          newValue: Any
        ): Boolean {
          val stringValue = newValue.toString()
          val listPreference = preference as ListPreference
          val index = listPreference.findIndexOfValue(stringValue)
          preferences.edit()
              .remove(AppPreferences.FONT_SIZE)
              .putInt(AppPreferences.FONT_SIZE, index)
              .apply()
          broadcastUpdateWidget()
          fontSizePreference.summary = fontSizePreference.entries[index]
          InAppMessage.showMessage(requireActivity(), R.string.text_size_updated_message)
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
        override fun onPreferenceChange(
          preference: Preference,
          newValue: Any
        ): Boolean {
          val stringValue = newValue.toString()
          val listPreference = preference as ListPreference
          val index = listPreference.findIndexOfValue(stringValue)
          preferences.edit()
              .putInt(AppPreferences.UPDATE_INTERVAL, index)
              .apply()
          broadcastUpdateWidget()
          refreshPreference.summary = refreshPreference.entries[index]
          InAppMessage.showMessage(requireActivity(), R.string.refresh_updated_message)
          return true
        }
      }
    }

    run {
      val startTimePref = findPreference(AppPreferences.START_TIME) as TimePreference
      startTimePref.summary = preferences.getString(AppPreferences.START_TIME, "09:30")
      startTimePref.onPreferenceChangeListener = object : DefaultPreferenceChangeListener() {
        override fun onPreferenceChange(
          preference: Preference,
          newValue: Any
        ): Boolean {
          val startTimez = appPreferences.timeAsIntArray(newValue.toString())
          val endTimez = appPreferences.endTime()
          if (endTimez[0] == startTimez[0] && endTimez[1] == startTimez[1]) {
            showDialog(getString(R.string.incorrect_time_update_error))
            return false
          } else {
            preferences.edit()
                .putString(AppPreferences.START_TIME, newValue.toString())
                .apply()
            startTimePref.summary = newValue.toString()
            stocksProvider.schedule()
            InAppMessage.showMessage(requireActivity(), R.string.start_time_updated)
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
        override fun onPreferenceChange(
          preference: Preference,
          newValue: Any
        ): Boolean {
          val endTimez = appPreferences.timeAsIntArray(newValue.toString())
          val startTimez = appPreferences.startTime()
          if (endTimez[0] == startTimez[0] && endTimez[1] == startTimez[1]) {
            showDialog(getString(R.string.incorrect_time_update_error))
            return false
          } else {
            preferences.edit()
                .putString(AppPreferences.END_TIME, newValue.toString())
                .apply()
            endTimePref.summary = newValue.toString()
            stocksProvider.schedule()
            InAppMessage.showMessage(requireActivity(), R.string.end_time_updated)
            return true
          }
        }
      }
    }

    run {
      val daysPreference = findPreference<MultiSelectListPreference>(AppPreferences.UPDATE_DAYS)
      val selectedDays = appPreferences.updateDaysRaw()
      daysPreference.summary = appPreferences.updateDays()
          .joinToString {
            it.getDisplayName(SHORT, Locale.getDefault())
          }
      daysPreference.values = selectedDays
      daysPreference.onPreferenceChangeListener = object : DefaultPreferenceChangeListener() {
        override fun onPreferenceChange(
          preference: Preference,
          newValue: Any
        ): Boolean {
          val selectedValues = newValue as Set<String>
          if (selectedValues.isEmpty()) {
            InAppMessage.showMessage(
                requireActivity(), R.string.days_updated_error_message, error = true
            )
            return false
          }
          appPreferences.setUpdateDays(selectedValues)
          daysPreference.summary = appPreferences.updateDays()
              .joinToString {
                it.getDisplayName(SHORT, Locale.getDefault())
              }
          stocksProvider.schedule()
          InAppMessage.showMessage(requireActivity(), R.string.days_updated_message)
          broadcastUpdateWidget()
          return true
        }
      }
    }

    run {
      val round2dpPref = findPreference<CheckBoxPreference>(AppPreferences.SETTING_ROUND_TWO_DP)
      round2dpPref.isChecked = appPreferences.roundToTwoDecimalPlaces()
      round2dpPref.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { _, newValue ->
          appPreferences.setRoundToTwoDecimalPlaces(newValue as Boolean)
          true
        }
    }
  }

  private fun <T : Preference> findPreference(key: String): T {
    return super.findPreference(key)!!
  }

  private fun needsPermissionGrant(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(
        requireActivity(),
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) != PackageManager.PERMISSION_GRANTED
  }

  private fun askForExternalStoragePermissions(reqCode: Int) {
    ActivityCompat.requestPermissions(
        requireActivity(),
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ), reqCode
    )
  }

  private fun exportAndShareTickers(file: File) {
    if (file.exists()) {
      shareTickers(file)
    } else {
      lifecycleScope.launch {
        val result = TickersExporter.exportTickers(file, stocksProvider.getTickers())
        if (result == null) {
          showDialog(getString(R.string.error_sharing))
          Timber.w(Throwable("Error sharing tickers"))
        } else {
          shareTickers(file)
        }
      }
    }
  }

  private fun exportAndShareTickers(uri: Uri) {
    lifecycleScope.launch {
      val result = TickersExporter.exportTickers(requireContext(), uri, stocksProvider.getTickers())
      if (result == null) {
        showDialog(getString(R.string.error_sharing))
        Timber.w(Throwable("Error sharing tickers"))
      } else {
        shareTickers(uri)
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.KITKAT)
  private fun launchExportIntent() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "application/json"
    }
    startActivityForResult(intent, REQCODE_FILE_WRITE)
  }

  @RequiresApi(Build.VERSION_CODES.KITKAT)
  private fun launchExportForShareIntent() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "text/plain"
    }
    startActivityForResult(intent, REQCODE_FILE_WRITE_SHARE)
  }

  private fun launchImportIntent() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      Intent(Intent.ACTION_OPEN_DOCUMENT)
    } else {
      Intent(Intent.ACTION_GET_CONTENT)
    }
    intent.type = "*/*"
    startActivityForResult(intent, REQCODE_FILE_READ)
  }

  private fun exportPortfolio(file: File) {
    lifecycleScope.launch {
      val result = PortfolioExporter.exportQuotes(file, stocksProvider.getPortfolio())
      if (result == null) {
        showDialog(getString(R.string.error_exporting))
        Timber.w(Throwable("Error exporting tickers"))
      } else {
        showDialog(getString(R.string.exported_to))
      }
    }
  }

  private fun exportPortfolio(uri: Uri) {
    lifecycleScope.launch {
      val result = PortfolioExporter.exportQuotes(requireContext(), uri, stocksProvider.getPortfolio())
      if (result == null) {
        showDialog(getString(R.string.error_exporting))
        Timber.w(Throwable("Error exporting tickers"))
      } else {
        showDialog(getString(R.string.exported_to))
      }
    }
  }

  private fun shareTickers(file: File) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>())
    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.my_stock_portfolio))
    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_email_subject))
    if (!file.exists() || !file.canRead()) {
      showDialog(getString(R.string.error_sharing))
      Timber.w(Throwable("Error sharing tickers"))
      return
    }
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", file)
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

  private fun shareTickers(uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf<String>())
    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.my_stock_portfolio))
    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_email_subject))
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

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      REQCODE_WRITE_EXTERNAL_STORAGE -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            launchExportIntent()
          } else {
            exportPortfolio(AppPreferences.portfolioFile)
          }
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
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            launchExportForShareIntent()
          } else {
            exportAndShareTickers(AppPreferences.tickersFile)
          }
        } else {
          showDialog(getString(R.string.cannot_share_msg))
        }
      }
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    if (requestCode == REQCODE_FILE_READ && resultCode == Activity.RESULT_OK) {
      val fileUri = data?.data
      if (fileUri != null) {
        val type = requireContext().contentResolver.getType(fileUri)
        val task: ImportTask = if ("text/plain" == type) {
          TickersImportTask(widgetDataProvider)
        } else {
          PortfolioImportTask(stocksProvider)
        }
        lifecycleScope.launch {
          val imported = task.import(requireContext(), fileUri)
          if (imported) {
            showDialog(getString(R.string.ticker_import_success))
          } else {
            showDialog(getString(R.string.ticker_import_fail))
          }
        }
      }
    } else if (requestCode == REQCODE_FILE_WRITE && resultCode == Activity.RESULT_OK) {
      val fileUri = data?.data
      if (fileUri != null) {
        exportPortfolio(fileUri)
      }
    }
    else if (requestCode == REQCODE_FILE_WRITE_SHARE && resultCode == Activity.RESULT_OK) {
      val fileUri = data?.data
      if (fileUri != null) {
        exportAndShareTickers(fileUri)
      }
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  private fun createTimePickerDialog(pref: TimePreference): Dialog {
    val listener = TimeSelectedListener(
        pref,
        if (pref.key == AppPreferences.START_TIME) R.string.start_time_updated
        else R.string.end_time_updated
    )
    if (pref.key == AppPreferences.START_TIME) {
      val startTime = appPreferences.startTime()
      pref.lastHour = startTime[0]
      pref.lastMinute = startTime[1]
    } else {
      val endTime = appPreferences.endTime()
      pref.lastHour = endTime[0]
      pref.lastMinute = endTime[1]
    }
    val dialog = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
      TimePickerDialog(
          context, R.style.AlertDialog, listener,
          pref.lastHour, pref.lastMinute, true
      )
    } else {
      // This is to fix a crash on Samsung devices running Android 6.0.1
      // See https://github.com/premnirmal/StockTicker/issues/80
      val timeSelectorLayout = LayoutInflater.from(requireContext())
          .inflate(R.layout.layout_time_selector, null)
      val hourText = timeSelectorLayout.findViewById<EditText>(R.id.hour_text)
      val minuteText = timeSelectorLayout.findViewById<EditText>(R.id.minute_text)
      hourText.setText(pref.lastHour.toString())
      hourText.setSelection(hourText.text.toString().length)
      minuteText.setText(pref.lastMinute.toString())
      minuteText.setSelection(minuteText.text.toString().length)
      AlertDialog.Builder(requireContext())
          .setView(timeSelectorLayout)
          .setPositiveButton(R.string.ok) { _, _ ->
            onTimeSet(
                pref, hourText.text.toString().toInt(), minuteText.text.toString().toInt(),
                if (pref.key == AppPreferences.START_TIME) R.string.start_time_updated
                else R.string.end_time_updated
            )
          }
          .setNegativeButton(R.string.cancel, null)
          .create()
    }
    dialog.setTitle(
        if (pref.key == AppPreferences.START_TIME) R.string.start_time else R.string.end_time
    )
    return dialog
  }

  private fun onTimeSet(
    preference: TimePreference,
    lastHour: Int,
    lastMinute: Int,
    messageRes: Int
  ) {
    if (lastHour > 23 || lastHour < 0 || lastMinute > 59 || lastMinute < 0) {
      InAppMessage.showMessage(requireActivity(), R.string.invalid_time, true)
      return
    }
    val hourString = if (lastHour < 10) "0$lastHour" else lastHour.toString()
    val minuteString = if (lastMinute < 10) "0$lastMinute" else lastMinute.toString()
    val time = "$hourString:$minuteString"
    val startTimez = appPreferences.timeAsIntArray(time)
    val endTimez = appPreferences.endTime()
    if (endTimez[0] == startTimez[0] && endTimez[1] == startTimez[1]) {
      showDialog(getString(R.string.incorrect_time_update_error))
    } else {
      preferences.edit()
          .putString(preference.key, time)
          .apply()
      preference.summary = time
      stocksProvider.schedule()
      InAppMessage.showMessage(requireActivity(), messageRes)
    }
  }

  inner class TimeSelectedListener(
    private val preference: TimePreference,
    private val messageRes: Int
  ) : TimePickerDialog.OnTimeSetListener {

    override fun onTimeSet(
      picker: TimePicker,
      hourOfDay: Int,
      minute: Int
    ) {
      val lastHour = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        picker.hour
      } else {
        picker.currentHour
      }
      val lastMinute = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        picker.minute
      } else {
        picker.currentMinute
      }
      onTimeSet(preference, lastHour, lastMinute, messageRes)
    }
  }
}
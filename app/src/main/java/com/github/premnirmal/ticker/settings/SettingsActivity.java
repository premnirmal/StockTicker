package com.github.premnirmal.ticker.settings;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.devpaul.filepickerlibrary.FilePickerActivity;
import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.widget.StockWidget;
import com.github.premnirmal.tickerwidget.BuildConfig;
import com.github.premnirmal.tickerwidget.R;

import javax.inject.Inject;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

/**
 * Created by premnirmal on 01/09/15.
 */
public class SettingsActivity extends PreferenceActivity {

    @Inject
    IStocksProvider stocksProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.inject(this);
        setContentView(R.layout.activity_preferences);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final Toolbar bar = (Toolbar) findViewById(R.id.toolbar);
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getListView().addFooterView(LayoutInflater.from(this).inflate(R.layout.preferences_footer, null, false));

        final TextView versionView = (TextView) findViewById(R.id.version);
        final SpannableStringBuilder sBuilder = new SpannableStringBuilder();
        sBuilder.append("Version " + BuildConfig.VERSION_NAME);
        final CalligraphyTypefaceSpan typefaceSpan = new CalligraphyTypefaceSpan(TypefaceUtils.load(getAssets(), "fonts/alegreya-black-italic.ttf"));
        sBuilder.setSpan(typefaceSpan, 0, sBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        versionView.setText(sBuilder);
        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.prefs);

        final SharedPreferences preferences = getSharedPreferences(Tools.PREFS_NAME, Context.MODE_PRIVATE);

        final Preference exportPref = findPreference("EXPORT");
        exportPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new FileExportTask() {
                    @Override
                    protected void onPostExecute(String result) {
                        if (result == null) {
                            showDialog(getString(R.string.error_exporting));
                        } else {
                            showDialog("Exported to " + result);
                        }
                    }
                }.execute(stocksProvider.getTickers().toArray());
                return true;
            }
        });

        final Preference importPref = findPreference("IMPORT");
        importPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Intent filePickerIntent = new Intent(SettingsActivity.this, FilePickerActivity.class);
                filePickerIntent.putExtra(FilePickerActivity.REQUEST_CODE, FilePickerActivity.REQUEST_FILE);
                filePickerIntent.putExtra(FilePickerActivity.INTENT_EXTRA_COLOR_ID, R.color.maroon);
                startActivityForResult(filePickerIntent, FilePickerActivity.REQUEST_FILE);
                return true;
            }
        });

        final ListPreference fontSizePreference = (ListPreference) findPreference(Tools.FONT_SIZE);
        final int size = preferences.getInt(Tools.FONT_SIZE, 1);
        fontSizePreference.setValueIndex(size);
        fontSizePreference.setSummary(fontSizePreference.getEntries()[size]);
        fontSizePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                final String stringValue = value.toString();
                final ListPreference listPreference = (ListPreference) preference;
                final int index = listPreference.findIndexOfValue(stringValue);
                preferences.edit().remove(Tools.FONT_SIZE).putInt(Tools.FONT_SIZE, index).commit();
                broadcastUpdateWidget();
                fontSizePreference.setSummary(fontSizePreference.getEntries()[index]);
                Toast.makeText(SettingsActivity.this, R.string.text_size_updated_message, Toast.LENGTH_SHORT).show();
                return true;
            }
        });


        final ListPreference bgPreference = (ListPreference) findPreference(Tools.WIDGET_BG);
        final int bgIndex = preferences.getInt(Tools.WIDGET_BG, 0);
        bgPreference.setValueIndex(bgIndex);
        bgPreference.setSummary(bgPreference.getEntries()[bgIndex]);
        bgPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                final String stringValue = value.toString();
                final ListPreference listPreference = (ListPreference) preference;
                final int index = listPreference.findIndexOfValue(stringValue);
                preferences.edit().putInt(Tools.WIDGET_BG, index).commit();
                broadcastUpdateWidget();
                bgPreference.setSummary(bgPreference.getEntries()[index]);
                Toast.makeText(SettingsActivity.this, R.string.bg_updated_message, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        final ListPreference refreshPreference = (ListPreference) findPreference(Tools.UPDATE_INTERVAL);
        final int refreshIndex = preferences.getInt(Tools.UPDATE_INTERVAL, 1);
        refreshPreference.setValueIndex(refreshIndex);
        refreshPreference.setSummary(refreshPreference.getEntries()[refreshIndex]);
        refreshPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                final String stringValue = value.toString();
                final ListPreference listPreference = (ListPreference) preference;
                final int index = listPreference.findIndexOfValue(stringValue);
                preferences.edit().putInt(Tools.UPDATE_INTERVAL, index).commit();
                broadcastUpdateWidget();
                refreshPreference.setSummary(refreshPreference.getEntries()[index]);
                Toast.makeText(SettingsActivity.this, R.string.refresh_updated_message, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        final CheckBoxPreference autoSortPreference = (CheckBoxPreference) findPreference(Tools.SETTING_AUTOSORT);
        final boolean autoSort = preferences.getBoolean(Tools.SETTING_AUTOSORT, false);
        autoSortPreference.setChecked(autoSort);
        autoSortPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                final boolean checked = (boolean) value;
                preferences.edit().putBoolean(Tools.SETTING_AUTOSORT, checked).commit();
                return true;
            }
        });
    }

    private void broadcastUpdateWidget() {
        final Intent intent = new Intent(getApplicationContext(), StockWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        final AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
        final int[] ids = widgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), StockWidget.class));
        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FilePickerActivity.REQUEST_FILE
                && resultCode == RESULT_OK) {
            final String filePath = data.
                    getStringExtra(FilePickerActivity.FILE_EXTRA_DATA_PATH);
            if (filePath != null) {
                new FileImportTask(stocksProvider) {
                    @Override
                    protected void onPostExecute(Boolean result) {
                        if (result) {
                            showDialog(getString(R.string.ticker_import_success));
                        } else {
                            showDialog(getString(R.string.ticker_import_fail));
                        }
                    }
                }.execute(filePath);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
package com.github.premnirmal.ticker.settings;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.premnirmal.ticker.CrashLogger;
import com.devpaul.filepickerlibrary.FilePickerActivity;
import com.github.premnirmal.ticker.Analytics;
import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.widget.StockWidget;
import com.github.premnirmal.tickerwidget.BuildConfig;
import com.github.premnirmal.tickerwidget.R;

import java.io.File;

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

    @Inject
    SharedPreferences preferences;

    private class DefaultPreferenceChangeListener implements Preference.OnPreferenceChangeListener {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            return false;
        }
    }


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

        {
            final Preference exportPref = findPreference("EXPORT");
            exportPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new FileExportTask() {
                        @Override
                        protected void onPostExecute(String result) {
                            if (result == null) {
                                showDialog(getString(R.string.error_exporting));
                                CrashLogger.logException(new Throwable("Error exporting tickers"));
                            } else {
                               showDialog("Exported to " + result);
                            }
                        }
                    }.execute(stocksProvider.getTickers().toArray());
                    return true;
                }
            });
        }

        {
            final Preference sharePref = findPreference("SHARE");
            sharePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Analytics.trackSettingsChange("SHARE", TextUtils.join(",", stocksProvider.getTickers().toArray()));
                    final File file = Tools.getTickersFile();
                    if(file.exists()) {
                        shareTickers();
                    } else {
                        new FileExportTask() {
                            @Override
                            protected void onPostExecute(String result) {
                                if (result == null) {
                                    showDialog(getString(R.string.error_sharing));
                                    CrashLogger.logException(new Throwable("Error sharing tickers"));
                                } else {
                                    shareTickers();
                                }
                            }
                        }.execute(stocksProvider.getTickers().toArray());
                    }
                    return true;
                }
            });
        }

        {
            final Preference importPref = findPreference("IMPORT");
            importPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Intent filePickerIntent = new Intent(SettingsActivity.this, FilePickerActivity.class);
                    filePickerIntent.putExtra(FilePickerActivity.REQUEST_CODE, FilePickerActivity.REQUEST_FILE);
                    filePickerIntent.putExtra(FilePickerActivity.INTENT_EXTRA_COLOR_ID, R.color.color_primary);
                    startActivityForResult(filePickerIntent, FilePickerActivity.REQUEST_FILE);
                    return true;
                }
            });
        }

        {
            final ListPreference fontSizePreference = (ListPreference) findPreference(Tools.FONT_SIZE);
            final int size = preferences.getInt(Tools.FONT_SIZE, 1);
            fontSizePreference.setValueIndex(size);
            fontSizePreference.setSummary(fontSizePreference.getEntries()[size]);
            fontSizePreference.setOnPreferenceChangeListener(new DefaultPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    super.onPreferenceChange(preference,value);
                    final String stringValue = value.toString();
                    final ListPreference listPreference = (ListPreference) preference;
                    final int index = listPreference.findIndexOfValue(stringValue);
                    preferences.edit().remove(Tools.FONT_SIZE).putInt(Tools.FONT_SIZE, index).apply();
                    broadcastUpdateWidget();
                    fontSizePreference.setSummary(fontSizePreference.getEntries()[index]);
                    Toast.makeText(SettingsActivity.this, R.string.text_size_updated_message, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        {
            final ListPreference bgPreference = (ListPreference) findPreference(Tools.WIDGET_BG);
            final int bgIndex = preferences.getInt(Tools.WIDGET_BG, 0);
            bgPreference.setValueIndex(bgIndex);
            bgPreference.setSummary(bgPreference.getEntries()[bgIndex]);
            bgPreference.setOnPreferenceChangeListener(new DefaultPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    super.onPreferenceChange(preference,value);
                    final String stringValue = value.toString();
                    final ListPreference listPreference = (ListPreference) preference;
                    final int index = listPreference.findIndexOfValue(stringValue);
                    preferences.edit().putInt(Tools.WIDGET_BG, index).apply();
                    broadcastUpdateWidget();
                    bgPreference.setSummary(bgPreference.getEntries()[index]);
                    Toast.makeText(SettingsActivity.this, R.string.bg_updated_message, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        {
            final ListPreference layoutTypePref = (ListPreference) findPreference(Tools.LAYOUT_TYPE);
            final int typeIndex = preferences.getInt(Tools.LAYOUT_TYPE, 0);
            layoutTypePref.setValueIndex(typeIndex);
            layoutTypePref.setSummary(layoutTypePref.getEntries()[typeIndex]);
            layoutTypePref.setOnPreferenceChangeListener(new DefaultPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    super.onPreferenceChange(preference,value);
                    final String stringValue = value.toString();
                    final ListPreference listPreference = (ListPreference) preference;
                    final int index = listPreference.findIndexOfValue(stringValue);
                    preferences.edit().putInt(Tools.LAYOUT_TYPE, index).apply();
                    broadcastUpdateWidget();
                    layoutTypePref.setSummary(layoutTypePref.getEntries()[index]);
                    Toast.makeText(SettingsActivity.this, R.string.layout_updated_message, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        {
            final ListPreference textColorPreference = (ListPreference) findPreference(Tools.TEXT_COLOR);
            final int colorIndex = preferences.getInt(Tools.TEXT_COLOR, 0);
            textColorPreference.setValueIndex(colorIndex);
            textColorPreference.setSummary(textColorPreference.getEntries()[colorIndex]);
            textColorPreference.setOnPreferenceChangeListener(new DefaultPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    super.onPreferenceChange(preference,value);
                    final String stringValue = value.toString();
                    final ListPreference listPreference = (ListPreference) preference;
                    final int index = listPreference.findIndexOfValue(stringValue);
                    preferences.edit().putInt(Tools.TEXT_COLOR, index).apply();
                    broadcastUpdateWidget();
                    CharSequence color = textColorPreference.getEntries()[index];
                    textColorPreference.setSummary(color);
                    Toast.makeText(SettingsActivity.this, R.string.text_coor_updated_message, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        {
            final ListPreference refreshPreference = (ListPreference) findPreference(Tools.UPDATE_INTERVAL);
            final int refreshIndex = preferences.getInt(Tools.UPDATE_INTERVAL, 1);
            refreshPreference.setValueIndex(refreshIndex);
            refreshPreference.setSummary(refreshPreference.getEntries()[refreshIndex]);
            refreshPreference.setOnPreferenceChangeListener(new DefaultPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    super.onPreferenceChange(preference,value);
                    final String stringValue = value.toString();
                    final ListPreference listPreference = (ListPreference) preference;
                    final int index = listPreference.findIndexOfValue(stringValue);
                    preferences.edit().putInt(Tools.UPDATE_INTERVAL, index).apply();
                    broadcastUpdateWidget();
                    refreshPreference.setSummary(refreshPreference.getEntries()[index]);
                    Toast.makeText(SettingsActivity.this, R.string.refresh_updated_message, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        {
            final CheckBoxPreference autoSortPreference = (CheckBoxPreference) findPreference(Tools.SETTING_AUTOSORT);
            final boolean autoSort = Tools.autoSortEnabled();
            autoSortPreference.setChecked(autoSort);
            autoSortPreference.setOnPreferenceChangeListener(new DefaultPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    super.onPreferenceChange(preference,value);
                    final boolean checked = (boolean) value;
                    preferences.edit().putBoolean(Tools.SETTING_AUTOSORT, checked).apply();
                    return true;
                }
            });
        }

        {
            final CheckBoxPreference boldChangePreference = (CheckBoxPreference) findPreference(Tools.BOLD_CHANGE);
            final boolean bold = Tools.boldEnabled();
            boldChangePreference.setChecked(bold);
            boldChangePreference.setOnPreferenceChangeListener(new DefaultPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    super.onPreferenceChange(preference,value);
                    final boolean checked = (boolean) value;
                    preferences.edit().putBoolean(Tools.BOLD_CHANGE, checked).apply();
                    return true;
                }
            });
        }

        {
            final TimePreference startTimePref = (TimePreference) findPreference(Tools.START_TIME);
            startTimePref.setSummary(preferences.getString(Tools.START_TIME, "09:30"));
            startTimePref.setOnPreferenceChangeListener(new DefaultPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    super.onPreferenceChange(preference,newValue);
                    preferences.edit().putString(Tools.START_TIME, newValue.toString()).apply();
                    startTimePref.setSummary(newValue.toString());
                    Toast.makeText(SettingsActivity.this, R.string.start_time_updated, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        {
            final TimePreference endTimePref = (TimePreference) findPreference(Tools.END_TIME);
            endTimePref.setSummary(preferences.getString(Tools.END_TIME, "16:30"));
            endTimePref.setOnPreferenceChangeListener(new DefaultPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    super.onPreferenceChange(preference,newValue);
                    preferences.edit().putString(Tools.END_TIME, newValue.toString()).apply();
                    endTimePref.setSummary(newValue.toString());
                    Toast.makeText(SettingsActivity.this, R.string.end_time_updated, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }

    private void shareTickers() {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.my_stock_portfolio));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_email_subject));
        final File file = Tools.getTickersFile();
        if (!file.exists() || !file.canRead()) {
            showDialog(getString(R.string.error_sharing));
            CrashLogger.logException(new Throwable("Error sharing tickers"));
            return;
        }
        final Uri uri = Uri.fromFile(file);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)));
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
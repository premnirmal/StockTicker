package com.github.premnirmal.ticker.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.devpaul.filepickerlibrary.FilePickerActivity;
import com.github.premnirmal.ticker.BuildConfig;
import com.github.premnirmal.ticker.R;
import com.github.premnirmal.ticker.StocksApp;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.model.IStocksProvider;

import javax.inject.Inject;

/**
 * Created by premnirmal on 12/22/14.
 */
public class SettingsActivity extends ActionBarActivity {

    @Inject
    IStocksProvider stocksProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((StocksApp) getApplicationContext()).inject(this);

        final SharedPreferences preferences = getSharedPreferences(Tools.PREFS_NAME, Context.MODE_PRIVATE);

        setContentView(R.layout.activity_settings);
        findViewById(R.id.action_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FileExportTask() {
                    @Override
                    protected void onPostExecute(String result) {
                        if (result == null) {
                            Toast.makeText(SettingsActivity.this, R.string.error_exporting, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SettingsActivity.this, "Exported to " + result, Toast.LENGTH_SHORT).show();
                        }
                    }
                }.execute(stocksProvider.getTickers().toArray());
            }
        });

        findViewById(R.id.action_import).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent filePickerIntent = new Intent(SettingsActivity.this, FilePickerActivity.class);
                filePickerIntent.putExtra(FilePickerActivity.REQUEST_CODE, FilePickerActivity.REQUEST_FILE);
                filePickerIntent.putExtra(FilePickerActivity.INTENT_EXTRA_COLOR_ID, R.color.maroon);
                startActivityForResult(filePickerIntent, FilePickerActivity.REQUEST_FILE);
            }
        });

        final CheckBox autoSortCheckbox = (CheckBox) findViewById(R.id.autosort_checkbox);
        autoSortCheckbox.setChecked(preferences.getBoolean(Tools.SETTING_AUTOSORT, false));
        autoSortCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(Tools.SETTING_AUTOSORT, isChecked).commit();
            }
        });

        findViewById(R.id.change_text_size).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SharedPreferences preferences = getSharedPreferences(Tools.PREFS_NAME, MODE_PRIVATE);
                final ListView view = new ListView(SettingsActivity.this);
                final int padding = (int) getResources().getDimension(R.dimen.text_padding);
                view.setPadding(padding, padding, padding, padding);
                view.setAdapter(ArrayAdapter.createFromResource(SettingsActivity.this, R.array.font_sizes, R.layout.textview));
                final AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)
                        .setView(view)
                        .create();
                view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        final int fontSizeDimen;
                        switch (position) {
                            case 1:
                                fontSizeDimen = R.integer.text_size_medium;
                                break;
                            case 2:
                                fontSizeDimen = R.integer.text_size_large;
                                break;
                            case 0:
                            default:
                                fontSizeDimen = R.integer.text_size_small;
                                break;
                        }
                        final int fontSize = getResources().getInteger(fontSizeDimen);
                        preferences.edit().remove(Tools.FONT_SIZE).putInt(Tools.FONT_SIZE, fontSize).commit();
                        dialog.dismiss();
                        Toast.makeText(SettingsActivity.this, R.string.text_size_updated_message, Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.show();

            }
        });

        ((TextView) findViewById(R.id.version)).setText("Version " + BuildConfig.VERSION_NAME);
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
                            Toast.makeText(SettingsActivity.this, R.string.ticker_import_success, Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(SettingsActivity.this, R.string.ticker_import_fail, Toast.LENGTH_SHORT).show();
                        }
                    }
                }.execute(filePath);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}

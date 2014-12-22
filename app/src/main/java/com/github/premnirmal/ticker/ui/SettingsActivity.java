package com.github.premnirmal.ticker.ui;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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
        setContentView(R.layout.activity_settings);
        findViewById(R.id.action_export).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        findViewById(R.id.action_import).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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

    }
}

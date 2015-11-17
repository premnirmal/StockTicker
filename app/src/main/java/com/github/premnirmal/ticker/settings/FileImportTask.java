package com.github.premnirmal.ticker.settings;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.github.premnirmal.ticker.Analytics;
import com.github.premnirmal.ticker.CrashLogger;
import com.github.premnirmal.ticker.model.IStocksProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * Created by premnirmal on 12/22/14.
 */
class FileImportTask extends AsyncTask<String, Void, Boolean> {

    private final IStocksProvider stocksProvider;

    FileImportTask(IStocksProvider stocksProvider) {
        this.stocksProvider = stocksProvider;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        URI uri;
        try {
            uri = new URI(params[0]);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        }
        if (uri == null || uri.getPath() == null || !uri.getPath().endsWith(".txt")) {
            return false;
        }

        final File tickersFile = new File(params[0]);
        boolean result = false;

        if (!tickersFile.exists()) {
            return false;
        }
        final StringBuilder text = new StringBuilder();
        try {
            final BufferedReader br = new BufferedReader(new FileReader(tickersFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            final String[] tickers = text.toString()
                    .replaceAll(" ", "")
                    .split(",");
            stocksProvider.addStocks(Arrays.asList(tickers));
            result = true;
            Analytics.trackSettingsChange("IMPORT", TextUtils.join(",", tickers));
        } catch (IOException e) {
            CrashLogger.logException(new RuntimeException(e));
            result = false;
        }

        return result;
    }
}

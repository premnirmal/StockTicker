package com.github.premnirmal.ticker.settings;

import android.os.AsyncTask;

import com.github.premnirmal.ticker.model.IStocksProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
            final String[] tickers = text.toString().replaceAll(" ","").trim().split(",");
            stocksProvider.addStocks(Arrays.asList(tickers));
            result = true;
        } catch (IOException e) {
            result = false;
        }

        return result;
    }
}

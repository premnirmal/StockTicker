package com.github.premnirmal.ticker.settings;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.github.premnirmal.ticker.Analytics;
import com.github.premnirmal.ticker.Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by premnirmal on 12/22/14.
 */
class FileExportTask extends AsyncTask<Object, Void, String> {

    @Override
    protected String doInBackground(Object... tickers) {
        final File file = Tools.getTickersFile();
        try {
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            }
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            for (Object ticker : tickers) {
                fileOutputStream.write((ticker + ",").getBytes());
            }
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            Crashlytics.logException(new RuntimeException(e));
            return null;
        } catch (IOException e) {
            Crashlytics.logException(new RuntimeException(e));
            return null;
        }
        Analytics.trackSettingsChange("EXPORT", TextUtils.join(",", tickers));
        return file.getPath();
    }
}

package com.github.premnirmal.ticker.settings

import android.os.AsyncTask
import android.text.TextUtils
import com.github.premnirmal.ticker.Tools
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import com.github.premnirmal.ticker.CrashLogger
import com.github.premnirmal.ticker.Analytics

/**
 * Created by premnirmal on 2/27/16.
 */
internal open class FileExportTask : AsyncTask<Any, Void, String>() {

    override fun doInBackground(vararg tickers: Any): String? {
        val file = Tools.tickersFile
        try {
            if (file.exists()) {
                file.delete()
                file.createNewFile()
            }
            val fileOutputStream = FileOutputStream(file)
            for (ticker in tickers) {
                fileOutputStream.write(("$ticker ,").toByteArray())
            }
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: FileNotFoundException) {
            CrashLogger.logException(RuntimeException(e))
            return null
        } catch (e: IOException) {
            CrashLogger.logException(RuntimeException(e))
            return null
        }

        Analytics.trackSettingsChange("EXPORT", TextUtils.join(",", tickers))
        return file.path
    }
}
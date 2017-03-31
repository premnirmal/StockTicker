package com.github.premnirmal.ticker.model

import android.content.Context
import com.github.premnirmal.ticker.network.data.Stock
import rx.Observable
import rx.schedulers.Schedulers
import java.io.*
import java.util.*

/**
 * Created by premnirmal on 2/28/16.
 */
internal class StocksStorage(val context: Context) {

  fun readSynchronous(): MutableList<Stock> {
    try {
      return readInternal()
    } catch (e: IOException) {
      e.printStackTrace()
      return ArrayList()
    }

  }

  fun save(stocks: MutableList<Stock>?): Observable<Boolean> {
    return Observable.fromCallable {
      var success = false
      val stocksFile = File(context.externalCacheDir, STOCKS_FILE)
      var fout: FileOutputStream? = null
      var oos: ObjectOutputStream? = null
      try {
        if (!stocksFile.exists()) {
          stocksFile.createNewFile()
        }
        fout = FileOutputStream(stocksFile)
        oos = ObjectOutputStream(fout)
        oos.writeObject(stocks)
        success = true
      } catch (e: FileNotFoundException) {
        e.printStackTrace()
      } catch (e: IOException) {
        e.printStackTrace()
      } catch (e: IllegalArgumentException) {
        e.printStackTrace()
      } finally {
        if (oos != null) {
          try {
            oos.flush()
            oos.close()
          } catch (e: IOException) {
            e.printStackTrace()
          }

        }
        if (fout != null) {
          try {
            fout.flush()
            fout.close()
          } catch (e: IOException) {
            e.printStackTrace()
          }

        }
      }
      success
    }
  }

  @Throws(IOException::class)
  private fun readInternal(): MutableList<Stock> {
    val stocksFile = File(context.externalCacheDir, STOCKS_FILE)
    var ois: ObjectInputStream? = null
    var fis: FileInputStream? = null
    try {
      fis = FileInputStream(stocksFile)
      ois = ObjectInputStream(fis)
      val stocks = ois.readObject() as MutableList<Stock>
      return stocks
    } catch (e: ClassNotFoundException) {
      e.printStackTrace()
    } catch (e: OptionalDataException) {
      e.printStackTrace()
    } catch (e: FileNotFoundException) {
      e.printStackTrace()
    } catch (e: StreamCorruptedException) {
      e.printStackTrace()
    } catch (e: IllegalArgumentException) {
      e.printStackTrace()
    } finally {
      if (ois != null) {
        ois.close()
      }
      if (fis != null) {
        fis.close()
      }
    }
    return ArrayList()
  }

  companion object {

    val STOCKS_FILE = "stocks.dat"
  }

}

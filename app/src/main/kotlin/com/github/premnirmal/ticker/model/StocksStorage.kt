package com.github.premnirmal.ticker.model

import android.content.Context
import com.github.premnirmal.ticker.network.Stock
import rx.Observable
import rx.Subscriber
import java.io.*
import java.util.*

/**
 * Created by premnirmal on 2/28/16.
 */
internal class StocksStorage(val context: Context) {

    fun save(stocks: MutableList<Stock>?): Observable<Boolean> {
        return Observable.create { subscriber ->
            if (stocks != null) {
                saveInternal(stocks).subscribe(object : Subscriber<Boolean>() {
                    override fun onCompleted() {

                    }

                    override fun onError(e: Throwable) {
                        subscriber.onError(e)
                    }

                    override fun onNext(aBoolean: Boolean?) {
                        subscriber.onNext(aBoolean)
                        subscriber.onCompleted()
                    }
                })
            } else {
                subscriber.onError(NullPointerException("Stock list was null!"))
            }
        }
    }

    fun readSynchronous(): MutableList<Stock> {
        try {
            return readInternal()
        } catch (e: IOException) {
            e.printStackTrace()
            return ArrayList()
        }

    }

    private fun saveInternal(stocks: MutableList<Stock>): Observable<Boolean> {
        return Observable.create { subscriber ->
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
            subscriber.onNext(success)
            subscriber.onCompleted()
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

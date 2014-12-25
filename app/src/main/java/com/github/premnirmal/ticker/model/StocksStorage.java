package com.github.premnirmal.ticker.model;

import android.os.Environment;

import com.github.premnirmal.ticker.network.Stock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by premnirmal on 12/25/14.
 */
class StocksStorage {

    static final String STOCKS_FILE = "stocks.dat";

    void save(List<Stock> stocks) {
        Observable.just(stocks)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                })
                .map(new Func1<List<Stock>, Boolean>() {
                    @Override
                    public Boolean call(List<Stock> stocks) {
                        try {
                            return saveInternal(stocks);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                })
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean success) {
                        if (!success) {
                            throw new RuntimeException("Error saving stock list");
                        }
                    }
                });
    }

    Observable<List<Stock>> read() {
        return Observable.create(new Observable.OnSubscribe<List<Stock>>() {
            @Override
            public void call(Subscriber<? super List<Stock>> subscriber) {
                try {
                    final List<Stock> stocks = readInternal();
                    subscriber.onNext(stocks);
                } catch (IOException e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }
            }
        });
    }

    private boolean saveInternal(List<Stock> stocks) throws IOException {
        boolean success = false;
        final File stocksFile = new File(Environment.getExternalStorageDirectory(), STOCKS_FILE);
        FileOutputStream fout = null;
        ObjectOutputStream oos = null;
        try {
            fout = new FileOutputStream(stocksFile);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(stocks);
            success = true;
        } finally {
            if (oos != null) {
                oos.flush();
                oos.close();
            }
            if (fout != null) {
                fout.flush();
                fout.close();
            }
        }
        return success;
    }

    private List<Stock> readInternal() throws IOException {
        final File stocksFile = new File(Environment.getExternalStorageDirectory(), STOCKS_FILE);
        ObjectInputStream ois = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(stocksFile);
            ois = new ObjectInputStream(fis);
            final List<Stock> stocks = (List<Stock>) ois.readObject();
            return stocks;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (OptionalDataException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ois != null) {
                ois.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
        return null;
    }

}

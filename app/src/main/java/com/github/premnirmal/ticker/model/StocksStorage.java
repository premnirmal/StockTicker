package com.github.premnirmal.ticker.model;

import android.content.Context;

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

/**
 * Created by premnirmal on 12/25/14.
 */
class StocksStorage {

    static final String STOCKS_FILE = "stocks.dat";

    final Context context;

    StocksStorage(Context context) {
        this.context = context;
    }

    Observable<Boolean> save(final List<Stock> stocks) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                saveInternal(stocks)
                        .subscribe(new Subscriber<Boolean>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                subscriber.onError(e);
                            }

                            @Override
                            public void onNext(Boolean aBoolean) {
                                subscriber.onNext(aBoolean);
                                subscriber.onCompleted();
                            }
                        });
            }
        });
    }

    List<Stock> readSynchronous() {
        try {
            return readInternal();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Observable<Boolean> saveInternal(final List<Stock> stocks) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                boolean success = false;
                final File stocksFile = new File(context.getExternalCacheDir(), STOCKS_FILE);
                FileOutputStream fout = null;
                ObjectOutputStream oos = null;
                try {
                    fout = new FileOutputStream(stocksFile);
                    oos = new ObjectOutputStream(fout);
                    oos.writeObject(stocks);
                    success = true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (oos != null) {
                        try {
                            oos.flush();
                            oos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fout != null) {
                        try {
                            fout.flush();
                            fout.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                subscriber.onNext(success);
                subscriber.onCompleted();
            }
        });
    }

    private List<Stock> readInternal() throws IOException {
        final File stocksFile = new File(context.getExternalCacheDir(), STOCKS_FILE);
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

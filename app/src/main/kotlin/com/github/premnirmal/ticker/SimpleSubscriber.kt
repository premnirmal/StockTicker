package com.github.premnirmal.ticker

import io.reactivex.Observer
import io.reactivex.disposables.Disposable


/**
 * Created by premnirmal on 4/14/16.
 */
open class SimpleSubscriber<T> : Observer<T> {

  override fun onComplete() {

  }

  override fun onSubscribe(d: Disposable) {

  }

  override fun onError(e: Throwable) {

  }

  override fun onNext(result: T) {

  }
}
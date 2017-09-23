package com.github.premnirmal.ticker.network

import io.reactivex.observers.DisposableObserver

/**
 * Created by premnirmal on 4/14/16.
 */
open class SimpleSubscriber<T> : DisposableObserver<T>() {

  override fun onComplete() {

  }

  override fun onError(e: Throwable) {

  }

  override fun onNext(result: T) {

  }
}
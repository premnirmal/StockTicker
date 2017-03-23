package com.github.premnirmal.ticker

import rx.Subscriber

/**
 * Created on 4/14/16.
 */
open class SimpleSubscriber<T> : Subscriber<T>() {

  override fun onError(e: Throwable) {

  }

  override fun onNext(result: T) {

  }

  override fun onCompleted() {

  }
}
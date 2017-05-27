package com.github.premnirmal.ticker.components

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject

/**
 * Created by premnirmal on 2/26/16.
 */
class RxBus {

  private val _bus = PublishSubject.create<Any>()

  fun post(o: Any) {
    _bus.onNext(o)
  }

  /**
   * Subscribe to any event
   */
  fun forAnyEvent(): Observable<Any> {
    return _bus.observeOn(AndroidSchedulers.mainThread())
  }

  /**
   * Subscribe to a specific event type
   */
  fun <T> forEventType(eventType: Class<T>): Observable<T> {
    return _bus.ofType(eventType).observeOn(AndroidSchedulers.mainThread())
  }
}
package com.github.premnirmal.ticker

import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject

/**
 * Created by premnirmal on 2/26/16.
 */
class RxBus {

    private val _bus = SerializedSubject(PublishSubject.create<Any>())

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
    fun <T> forEventType(eventType: Class<T>) : Observable<T> {
        return _bus.ofType(eventType).observeOn(AndroidSchedulers.mainThread())
    }
}
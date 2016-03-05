package com.github.premnirmal.ticker

import rx.Observable
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

    fun toObserverable(): Observable<Any> {
        return _bus
    }
}
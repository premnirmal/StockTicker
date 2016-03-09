package com.github.premnirmal.ticker.portfolio

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.AndroidRuntimeException
import android.view.View
import rx.Observable
import rx.android.app.AppObservable
import rx.android.lifecycle.LifecycleEvent
import rx.android.lifecycle.LifecycleObservable
import rx.subjects.BehaviorSubject

/**
 * Created by premnirmal on 2/25/16.
 */
abstract class BaseFragment : Fragment() {

    private val lifecycleSubject = BehaviorSubject.create<LifecycleEvent>()

    private var called: Boolean = false

    protected fun lifecycle(): Observable<LifecycleEvent> {
        return lifecycleSubject.asObservable()
    }

    override fun onAttach(activity: android.app.Activity?) {
        super.onAttach(activity)
        lifecycleSubject.onNext(LifecycleEvent.ATTACH)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleSubject.onNext(LifecycleEvent.CREATE)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleSubject.onNext(LifecycleEvent.CREATE_VIEW)
        called = true
    }

    override fun onStart() {
        super.onStart()
        lifecycleSubject.onNext(LifecycleEvent.START)
    }

    override fun onResume() {
        if (!called) {
            throw AndroidRuntimeException("You didn't call super.onViewCreated() when in " + javaClass.simpleName)
        }
        super.onResume()
        lifecycleSubject.onNext(LifecycleEvent.RESUME)
    }

    override fun onPause() {
        lifecycleSubject.onNext(LifecycleEvent.PAUSE)
        super.onPause()
    }

    override fun onStop() {
        lifecycleSubject.onNext(LifecycleEvent.STOP)
        super.onStop()
    }

    override fun onDestroyView() {
        lifecycleSubject.onNext(LifecycleEvent.DESTROY_VIEW)
        super.onDestroyView()
    }

    override fun onDetach() {
        lifecycleSubject.onNext(LifecycleEvent.DETACH)
        super.onDetach()
    }

    override fun onDestroy() {
        lifecycleSubject.onNext(LifecycleEvent.DESTROY)
        super.onDestroy()
    }

    protected fun <T> bind(observable: Observable<T>): Observable<T> {
        val boundObservable = AppObservable.bindFragment(this, observable)
        return LifecycleObservable.bindFragmentLifecycle(lifecycle(), boundObservable)
    }
}

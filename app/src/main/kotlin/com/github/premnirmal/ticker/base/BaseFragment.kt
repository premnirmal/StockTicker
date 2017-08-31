package com.github.premnirmal.ticker.base

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.AndroidRuntimeException
import android.view.View
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.android.FragmentEvent.ATTACH
import com.trello.rxlifecycle2.android.FragmentEvent.CREATE
import com.trello.rxlifecycle2.android.FragmentEvent.CREATE_VIEW
import com.trello.rxlifecycle2.android.FragmentEvent.DESTROY
import com.trello.rxlifecycle2.android.FragmentEvent.DESTROY_VIEW
import com.trello.rxlifecycle2.android.FragmentEvent.DETACH
import com.trello.rxlifecycle2.android.FragmentEvent.PAUSE
import com.trello.rxlifecycle2.android.FragmentEvent.RESUME
import com.trello.rxlifecycle2.android.FragmentEvent.START
import com.trello.rxlifecycle2.android.FragmentEvent.STOP
import com.trello.rxlifecycle2.android.RxLifecycleAndroid
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/**
 * Created by premnirmal on 2/25/16.
 */
abstract class BaseFragment : Fragment() {

  private val lifecycleSubject = BehaviorSubject.create<FragmentEvent>()

  private var called: Boolean = false

  protected fun lifecycle(): Observable<FragmentEvent> = lifecycleSubject

  override fun onAttach(activity: Activity?) {
    super.onAttach(activity)
    lifecycleSubject.onNext(ATTACH)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycleSubject.onNext(CREATE)
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleSubject.onNext(CREATE_VIEW)
    called = true
  }

  override fun onStart() {
    super.onStart()
    lifecycleSubject.onNext(START)
  }

  override fun onResume() {
    if (!called) {
      throw AndroidRuntimeException(
          "You didn't call super.onViewCreated() when in " + javaClass.simpleName)
    }
    super.onResume()
    lifecycleSubject.onNext(RESUME)
  }

  override fun onPause() {
    lifecycleSubject.onNext(PAUSE)
    super.onPause()
  }

  override fun onStop() {
    lifecycleSubject.onNext(STOP)
    super.onStop()
  }

  override fun onDestroyView() {
    lifecycleSubject.onNext(DESTROY_VIEW)
    super.onDestroyView()
  }

  override fun onDetach() {
    lifecycleSubject.onNext(DETACH)
    super.onDetach()
  }

  override fun onDestroy() {
    lifecycleSubject.onNext(DESTROY)
    super.onDestroy()
  }

  /**
   * Using this to automatically unsubscribe from observables on lifecycle events
   */
  protected fun <T> bind(observable: Observable<T>): Observable<T> =
      observable.compose(RxLifecycleAndroid.bindFragment(lifecycle()))
}

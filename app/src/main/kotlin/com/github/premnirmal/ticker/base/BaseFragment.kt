package com.github.premnirmal.ticker.base

import android.content.Context
import android.os.Bundle
import android.util.AndroidRuntimeException
import android.view.View
import androidx.fragment.app.Fragment
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
abstract class BaseFragment : Fragment(), FragmentLifeCycleOwner {

  override val lifecycle: BehaviorSubject<FragmentEvent> = BehaviorSubject.create<FragmentEvent>()

  private var called: Boolean = false

  override fun onAttach(context: Context) {
    super.onAttach(context)
    lifecycle.onNext(ATTACH)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycle.onNext(CREATE)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycle.onNext(CREATE_VIEW)
    called = true
  }

  override fun onStart() {
    super.onStart()
    lifecycle.onNext(START)
  }

  override fun onResume() {
    if (!called) {
      throw AndroidRuntimeException(
          "You didn't call super.onViewCreated() when in " + javaClass.simpleName)
    }
    super.onResume()
    lifecycle.onNext(RESUME)
  }

  override fun onPause() {
    lifecycle.onNext(PAUSE)
    super.onPause()
  }

  override fun onStop() {
    lifecycle.onNext(STOP)
    super.onStop()
  }

  override fun onDestroyView() {
    lifecycle.onNext(DESTROY_VIEW)
    super.onDestroyView()
  }

  override fun onDetach() {
    lifecycle.onNext(DETACH)
    super.onDetach()
  }

  override fun onDestroy() {
    lifecycle.onNext(DESTROY)
    super.onDestroy()
  }

  /**
   * Using this to automatically unsubscribe from observables on lifecycle events
   */
  protected fun <T> bind(observable: Observable<T>): Observable<T> =
    observable.compose(RxLifecycleAndroid.bindFragment(lifecycle))
}

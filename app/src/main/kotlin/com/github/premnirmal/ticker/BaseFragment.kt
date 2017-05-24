package com.github.premnirmal.ticker

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.AndroidRuntimeException
import android.view.View
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.android.RxLifecycleAndroid
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

/**
 * Created by premnirmal on 2/25/16.
 */
abstract class BaseFragment : Fragment() {

  private val lifecycleSubject = BehaviorSubject.create<FragmentEvent>()

  private var called: Boolean = false

  protected fun lifecycle(): Observable<FragmentEvent> {
    return lifecycleSubject
  }

  override fun onAttach(activity: Activity?) {
    super.onAttach(activity)
    lifecycleSubject.onNext(FragmentEvent.ATTACH)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycleSubject.onNext(FragmentEvent.CREATE)
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleSubject.onNext(FragmentEvent.CREATE_VIEW)
    called = true
  }

  override fun onStart() {
    super.onStart()
    lifecycleSubject.onNext(FragmentEvent.START)
  }

  override fun onResume() {
    if (!called) {
      throw AndroidRuntimeException(
          "You didn't call super.onViewCreated() when in " + javaClass.simpleName)
    }
    super.onResume()
    lifecycleSubject.onNext(FragmentEvent.RESUME)
  }

  override fun onPause() {
    lifecycleSubject.onNext(FragmentEvent.PAUSE)
    super.onPause()
  }

  override fun onStop() {
    lifecycleSubject.onNext(FragmentEvent.STOP)
    super.onStop()
  }

  override fun onDestroyView() {
    lifecycleSubject.onNext(FragmentEvent.DESTROY_VIEW)
    super.onDestroyView()
  }

  override fun onDetach() {
    lifecycleSubject.onNext(FragmentEvent.DETACH)
    super.onDetach()
  }

  override fun onDestroy() {
    lifecycleSubject.onNext(FragmentEvent.DESTROY)
    super.onDestroy()
  }

  /**
   * Using this to automatically unsubscribe from observables on lifecycle events
   */
  protected fun <T> bind(observable: Observable<T>): Observable<T> {
    return observable.compose(RxLifecycleAndroid.bindFragment(lifecycle()));
  }

  // dialog


  protected fun showDialog(message: String): AlertDialog {
    val activity = activity as BaseActivity
    return activity.showDialog(message)
  }

  protected fun showDialog(title: String, message: String): AlertDialog {
    val activity = activity as BaseActivity
    return activity.showDialog(title, message)
  }

  protected fun showDialog(message: String,
      listener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() }): AlertDialog {
    val activity = activity as BaseActivity
    return activity.showDialog(message, listener)
  }

  protected fun showDialog(message: String, positiveOnClick: DialogInterface.OnClickListener,
      negativeOnClick: DialogInterface.OnClickListener): AlertDialog {
    val activity = activity as BaseActivity
    return activity.showDialog(message, positiveOnClick, negativeOnClick)
  }

  protected fun showDialog(message: String, cancelable: Boolean,
      positiveOnClick: DialogInterface.OnClickListener,
      negativeOnClick: DialogInterface.OnClickListener): AlertDialog {
    val activity = activity as BaseActivity
    return activity.showDialog(message, cancelable, positiveOnClick, negativeOnClick)
  }
}

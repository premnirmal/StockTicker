package com.github.premnirmal.ticker

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.github.premnirmal.ticker.events.ErrorEvent
import com.trello.rxlifecycle.ActivityEvent
import com.trello.rxlifecycle.RxLifecycle
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.BehaviorSubject
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
abstract class BaseActivity : AppCompatActivity() {

  private val lifecycleSubject = BehaviorSubject.create<ActivityEvent>()

  @Inject lateinit var bus: RxBus

  private fun lifecycle(): Observable<ActivityEvent> {
    return lifecycleSubject.asObservable()
  }

  /**
   * Using this to automatically unsubscribe from observables on lifecycle events
   */
  protected fun <T> bind(observable: Observable<T>): Observable<T> {
    return observable.compose(RxLifecycle.bindActivity<T>(lifecycle()))
  }

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
    super.onCreate(savedInstanceState, persistentState)
    lifecycleSubject.onNext(ActivityEvent.CREATE)
  }

  override fun onStart() {
    super.onStart()
    lifecycleSubject.onNext(ActivityEvent.START)
  }

  override fun onStop() {
    super.onStop()
    lifecycleSubject.onNext(ActivityEvent.STOP)
  }

  override fun onDestroy() {
    super.onDestroy()
    lifecycleSubject.onNext(ActivityEvent.DESTROY)
  }

  override fun onResume() {
    super.onResume()
    lifecycleSubject.onNext(ActivityEvent.RESUME)
    bind(bus.forEventType(ErrorEvent::class.java))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { event ->
          showDialog(event.message)
        }
  }

  override fun onPause() {
    super.onPause()
    lifecycleSubject.onNext(ActivityEvent.PAUSE)
  }

  protected fun showDialog(message: String): AlertDialog {
    return AlertDialog.Builder(this).setMessage(message).setCancelable(false)
        .setNeutralButton("OK", { _: DialogInterface, _: Int -> }).show()
  }

  protected fun showDialog(message: String,
      listener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() }): AlertDialog {
    return AlertDialog.Builder(this).setMessage(message).setCancelable(false).setNeutralButton("OK",
        listener).show()
  }

  protected fun showDialog(message: String, positiveOnClick: DialogInterface.OnClickListener,
      negativeOnClick: DialogInterface.OnClickListener): AlertDialog {
    return showDialog(message, false, positiveOnClick, negativeOnClick)
  }

  protected fun showDialog(message: String, cancelable: Boolean,
      positiveOnClick: DialogInterface.OnClickListener,
      negativeOnClick: DialogInterface.OnClickListener): AlertDialog {
    return AlertDialog.Builder(this).setMessage(message).setCancelable(
        cancelable).setPositiveButton(
        "YES", positiveOnClick).setNegativeButton("NO", negativeOnClick).show()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressed()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  fun updateToolbar(toolbar: Toolbar) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      toolbar.setPadding(toolbar.paddingLeft, Tools.getStatusBarHeight(this),
          toolbar.paddingRight, toolbar.paddingBottom)
    }
  }
}

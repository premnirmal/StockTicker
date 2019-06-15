package com.github.premnirmal.ticker.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.RxBus
import com.github.premnirmal.ticker.events.ErrorEvent
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.tickerwidget.R
import com.trello.rxlifecycle3.android.ActivityEvent
import com.trello.rxlifecycle3.android.RxLifecycleAndroid
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
abstract class BaseActivity : AppCompatActivity() {

  private val lifecycleSubject = BehaviorSubject.create<ActivityEvent>()

  @Inject internal lateinit var bus: RxBus

  private fun lifecycle(): Observable<ActivityEvent> = lifecycleSubject

  /**
   * Using this to automatically unsubscribe from observables on lifecycle events
   */
  protected fun <T> bind(observable: Observable<T>): Observable<T> =
    observable.compose(RxLifecycleAndroid.bindActivity(lifecycle()))

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
  }

  override fun onCreate(
    savedInstanceState: Bundle?,
    persistentState: PersistableBundle?
  ) {
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
    bind(bus.forEventType(ErrorEvent::class.java)).observeOn(AndroidSchedulers.mainThread())
        .subscribe { event ->
          showDialog(event.message)
        }
  }

  override fun onPause() {
    super.onPause()
    lifecycleSubject.onNext(ActivityEvent.PAUSE)
  }

  override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressed()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  fun updateToolbar(toolbar: androidx.appcompat.widget.Toolbar) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      toolbar.setPadding(
          toolbar.paddingLeft, getStatusBarHeight(), toolbar.paddingRight,
          toolbar.paddingBottom
      )
    }
  }

  protected fun showErrorAndFinish() {
    InAppMessage.showToast(this, R.string.error_symbol)
    finish()
  }

  protected fun dismissKeyboard() {
    val view = currentFocus
    if (view is TextView) {
      val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
  }
}

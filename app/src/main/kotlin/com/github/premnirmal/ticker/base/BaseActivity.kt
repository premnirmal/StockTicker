package com.github.premnirmal.ticker.base

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.RxBus
import com.github.premnirmal.ticker.events.ErrorEvent
import com.github.premnirmal.tickerwidget.R
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.android.RxLifecycleAndroid
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
abstract class BaseActivity : AppCompatActivity() {

  companion object {
    val EXTRA_CENTER_X = "centerX"
    val EXTRA_CENTER_Y = "centerY"

    fun Context.isNetworkOnline(): Boolean {
      try {
        val connectivityManager = this.getSystemService(
            Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val i = connectivityManager.activeNetworkInfo ?: return false
        if (!i.isConnected) return false
        if (!i.isAvailable) return false
        return true
      } catch (e: Exception) {
        e.printStackTrace()
        return false
      }
    }

    fun Context.getStatusBarHeight(): Int {
      val result: Int
      val resourceId: Int = this.resources.getIdentifier("status_bar_height", "dimen",
          "android")
      if (resourceId > 0) {
        result = this.resources.getDimensionPixelSize(resourceId)
      } else {
        result = 0
      }
      return result
    }

    fun Context.getFontSize(): Float {
      val size = AppPreferences.INSTANCE.sharedPreferences.getInt(AppPreferences.FONT_SIZE, 1)
      when (size) {
        0 -> return this.resources.getInteger(R.integer.text_size_small).toFloat()
        2 -> return this.resources.getInteger(R.integer.text_size_large).toFloat()
        else -> return this.resources.getInteger(R.integer.text_size_medium).toFloat()
      }
    }
  }

  private val lifecycleSubject = BehaviorSubject.create<ActivityEvent>()

  @Inject lateinit internal var bus: RxBus

  private fun lifecycle(): Observable<ActivityEvent> {
    return lifecycleSubject
  }

  /**
   * Using this to automatically unsubscribe from observables on lifecycle events
   */
  protected fun <T> bind(observable: Observable<T>): Observable<T> {
    return observable.compose(RxLifecycleAndroid.bindActivity(lifecycle()))
  }

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper.wrap(newBase))
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

  fun showDialog(message: String): AlertDialog {
    return AlertDialog.Builder(this).setMessage(message).setCancelable(false)
        .setNeutralButton("OK", { dialog: DialogInterface, _: Int -> dialog.dismiss() }).show()
  }

  fun showDialog(title: String, message: String): AlertDialog {
    return AlertDialog.Builder(this).setTitle(title).setMessage(message).setCancelable(false)
        .setNeutralButton("OK", { dialog: DialogInterface, _: Int -> dialog.dismiss() }).show()
  }

  fun showDialog(message: String,
      listener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() }): AlertDialog {
    return AlertDialog.Builder(this).setMessage(message).setCancelable(false).setNeutralButton("OK",
        listener).show()
  }

  fun showDialog(message: String, positiveOnClick: DialogInterface.OnClickListener,
      negativeOnClick: DialogInterface.OnClickListener): AlertDialog {
    return showDialog(message, false, positiveOnClick, negativeOnClick)
  }

  fun showDialog(message: String, cancelable: Boolean,
      positiveOnClick: DialogInterface.OnClickListener,
      negativeOnClick: DialogInterface.OnClickListener): AlertDialog {
    return AlertDialog.Builder(this).setMessage(message).setCancelable(
        cancelable).setPositiveButton(
        "YES", positiveOnClick).setNegativeButton("NO", negativeOnClick).show()
  }

  override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressed()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  fun updateToolbar(toolbar: android.support.v7.widget.Toolbar) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      toolbar.setPadding(toolbar.paddingLeft, getStatusBarHeight(),
          toolbar.paddingRight, toolbar.paddingBottom)
    }
  }
}

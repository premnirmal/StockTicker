package com.github.premnirmal.ticker.base

import android.support.v4.app.Fragment
import com.trello.rxlifecycle2.android.FragmentEvent
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface FragmentLifeCycleOwner {
  val lifecycle: BehaviorSubject<FragmentEvent>
}

class LifeCycleDelegate<T>(lifecycleParent: FragmentLifeCycleOwner, frag: Fragment) : ReadWriteProperty<Fragment, T> {

  private var value: T? = null
  private var fragment: Fragment? = null

  init {
    fragment = frag
    lifecycleParent.lifecycle.subscribe {
      Timber.d("Event: ${it.name}")
      if (it === FragmentEvent.ATTACH) {
        value = fragment!!.activity as T?
      } else if (it === FragmentEvent.DETACH) {
        value = null
        fragment = null
      }
    }
  }

  override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
    return value!!
  }

  override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
    this.value = value
  }
}
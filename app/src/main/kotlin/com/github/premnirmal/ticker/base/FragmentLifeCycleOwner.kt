package com.github.premnirmal.ticker.base

import android.app.Activity
import android.support.v4.app.Fragment
import com.trello.rxlifecycle2.android.FragmentEvent
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface FragmentLifeCycleOwner {
  val lifecycle: BehaviorSubject<FragmentEvent>
  fun getActivity(): Activity?
  fun getParentFragment(): Fragment?
}

class ParentFragmentDelegate<T>(lifecycleParent: FragmentLifeCycleOwner) :
    LifeCycleDelegate<T>(lifecycleParent) {
  override fun getParent(owner: FragmentLifeCycleOwner): T? = owner.getParentFragment() as? T
}

class ParentActivityDelegate<T>(lifecycleParent: FragmentLifeCycleOwner) :
    LifeCycleDelegate<T>(lifecycleParent) {
  override fun getParent(owner: FragmentLifeCycleOwner): T? = owner.getActivity() as? T
}

abstract class LifeCycleDelegate<T>(lifecycleParent: FragmentLifeCycleOwner) :
    ReadOnlyProperty<Fragment, T> {

  private var value: T? = null

  init {
    lifecycleParent.lifecycle.subscribe {
      Timber.d("Event: ${it.name}")
      if (it === FragmentEvent.ATTACH) {
        value = getParent(lifecycleParent)
      } else if (it === FragmentEvent.DETACH) {
        value = null
      }
    }
  }

  abstract fun getParent(owner: FragmentLifeCycleOwner): T?

  override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
    return value!!
  }
}
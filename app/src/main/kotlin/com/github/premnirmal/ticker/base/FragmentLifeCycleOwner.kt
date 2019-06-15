package com.github.premnirmal.ticker.base

import android.app.Activity
import com.trello.rxlifecycle3.android.FragmentEvent
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface FragmentLifeCycleOwner {
  val lifecycle: BehaviorSubject<FragmentEvent>
  fun getActivity(): Activity?
  fun getParentFragment(): androidx.fragment.app.Fragment?
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
    ReadOnlyProperty<androidx.fragment.app.Fragment, T> {

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

  override fun getValue(
    thisRef: androidx.fragment.app.Fragment,
    property: KProperty<*>
  ): T {
    return value!!
  }
}
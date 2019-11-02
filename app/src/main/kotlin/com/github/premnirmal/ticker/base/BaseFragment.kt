package com.github.premnirmal.ticker.base

import androidx.fragment.app.Fragment
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.Injector
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
abstract class BaseFragment : Fragment() {

  protected val analytics: Analytics
    get() = holder.analytics
  private val holder: InjectionHolder by lazy { InjectionHolder() }

  abstract val simpleName: String

  class InjectionHolder {
    @Inject internal lateinit var analytics: Analytics

    init {
      Injector.appComponent.inject(this)
    }
  }
}

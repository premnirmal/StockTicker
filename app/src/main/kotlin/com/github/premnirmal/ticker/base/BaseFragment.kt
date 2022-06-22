package com.github.premnirmal.ticker.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.Injector
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
abstract class BaseFragment<T: ViewBinding> : Fragment() {

  protected val analytics: Analytics
    get() = holder.analytics
  private val holder: InjectionHolder by lazy { InjectionHolder() }

  abstract val simpleName: String
  abstract val binding: T

  final override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return binding.root
  }

  class InjectionHolder {
    @Inject internal lateinit var analytics: Analytics

    init {
      Injector.appComponent.inject(this)
    }
  }
}

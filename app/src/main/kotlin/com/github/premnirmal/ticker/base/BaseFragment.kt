package com.github.premnirmal.ticker.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.inflateBinding
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
abstract class BaseFragment<T: ViewBinding> : Fragment() {

  protected val analytics: Analytics
    get() = holder.analytics
  private val holder: InjectionHolder by lazy { InjectionHolder() }

  abstract val simpleName: String
  private var _binding: T? = null
  val binding: T
    get() = _binding!!

  final override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = inflateBinding(layoutInflater) as T
    return binding.root
  }

  class InjectionHolder {
    @Inject internal lateinit var analytics: Analytics

    init {
      Injector.appComponent.inject(this)
    }
  }
}

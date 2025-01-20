package com.github.premnirmal.ticker.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsParentFragment : BaseFragment<FragmentSettingsBinding>(), ChildFragment {
  override val simpleName: String = "SettingsParentFragment"
  override val binding: (FragmentSettingsBinding) by viewBinding(FragmentSettingsBinding::inflate)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    if (savedInstanceState == null) {
      childFragmentManager.beginTransaction()
          .add(R.id.child_fragment_container, SettingsFragment())
          .commit()
    }
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
      view.findViewById<Toolbar>(R.id.toolbar)
          .updateLayoutParams<ViewGroup.MarginLayoutParams> {
            this.topMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
          }
      insets
    }
  }
  // ChildFragment

  override fun scrollToTop() {
    (childFragmentManager.findFragmentById(R.id.child_fragment_container) as ChildFragment).scrollToTop()
  }
}
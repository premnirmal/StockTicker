package com.github.premnirmal.ticker.settings

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.FragmentSettingsBinding

class SettingsParentFragment : BaseFragment<FragmentSettingsBinding>(), ChildFragment {
  override val simpleName = "SettingsParentFragment"

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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      view.findViewById<View>(R.id.fake_status_bar)
          .updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().getStatusBarHeight()
          }
    }
  }
  // ChildFragment

  override fun scrollToTop() {
    (childFragmentManager.findFragmentById(R.id.child_fragment_container) as ChildFragment).scrollToTop()
  }
}
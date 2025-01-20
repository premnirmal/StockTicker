package com.github.premnirmal.ticker.ui

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.fragment.app.findFragment

@Composable
fun FragmentContainer(
  modifier: Modifier = Modifier,
  commit: FragmentTransaction.(containerId: Int) -> Unit,
  onFragmentChange: (Int, Fragment) -> Unit
) {
  val localView = LocalView.current
  val parentFragment = remember(localView) {
    try {
      localView.findFragment<Fragment>()
    } catch (e: IllegalStateException) {
      // findFragment throws if no parent fragment is found
      null
    }
  }
  val containerId = rememberSaveable {
    val id = View.generateViewId()
    mutableStateOf(id)
  }
  val container = remember {
    mutableStateOf<FragmentContainerView?>(null)
  }
  val viewBlock: (Context) -> View = remember(localView) {
    { context ->
      FragmentContainerView(context)
          .apply { id = containerId.value }
          .also {
            val fragmentManager = parentFragment?.childFragmentManager
                ?: (context as? FragmentActivity)?.supportFragmentManager
            fragmentManager?.commit { commit(it.id) }
            container.value = it
            val fragment = fragmentManager?.findFragmentById(container.value?.id ?: 0)
            fragment?.let {
              onFragmentChange(containerId.value, fragment)
            }
          }
    }
  }
  AndroidView(
      modifier = modifier,
      factory = viewBlock,
      update = {}
  )

  val localContext = LocalContext.current
  DisposableEffect(localView, localContext, container) {
    onDispose {
      val fragmentManager =
        parentFragment?.childFragmentManager
            ?: (localContext as?
                FragmentActivity)?.supportFragmentManager
      val existingFragment = fragmentManager?.findFragmentById(container.value?.id ?: 0)
      if (existingFragment != null &&
          !fragmentManager.isStateSaved
      ) {
        fragmentManager.commit {
          remove(existingFragment)
        }
      }
    }
  }
}
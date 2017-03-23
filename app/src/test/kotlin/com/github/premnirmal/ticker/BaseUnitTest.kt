package com.github.premnirmal.ticker

import com.github.premnirmal.tickerwidget.R
import junit.framework.TestCase
import org.robolectric.Robolectric
import org.robolectric.util.ActivityController

/**
 * Created by premnirmal on 3/22/17.
 */
abstract class BaseUnitTest : TestCase() {

  /**
   * Attach a fragment to a new instance of the given activity class.
   *
   * @param activityClass The activity to build and attach the fragment to.
   * @param fragment Fragment to add to the activity.
   */
  fun attachFragmentToActivity(activityClass: Class<out BaseActivity>, fragment: BaseFragment)
      : ActivityController<out BaseActivity> {
    val controller = Robolectric.buildActivity(activityClass).create().start().resume()
    val activity = controller.get()
    val fm = activity.supportFragmentManager
    fm.beginTransaction().add(R.id.fragment_container, fragment).commit()
    return controller
  }
}
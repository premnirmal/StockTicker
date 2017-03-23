package com.github.premnirmal.ticker

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Created by premnirmal on 3/22/17.
 */
@RunWith(RobolectricTestRunner::class)
class ParanormalActivityTest()
  : BaseActivityUnitTest<ParanormalActivity>(ParanormalActivity::class.java, true) {

  @Test
  fun testActivityReady() {
    assertTrue(true)
  }
}
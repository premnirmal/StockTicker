package com.github.premnirmal.ticker

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by premnirmal on 3/22/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ParanormalActivityTest() :
    BaseActivityUnitTest<ParanormalActivity>(ParanormalActivity::class.java, true) {

  @Test
  fun testActivityReady() {
    assertTrue(true)
  }
}
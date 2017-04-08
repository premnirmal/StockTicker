package com.github.premnirmal.ticker.portfolio

import com.github.premnirmal.ticker.BaseUnitTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

/**
 * Created by premnirmal on 2/25/16.
 */
class PortfolioFragmentTest : BaseUnitTest() {

  var fragment: PortfolioFragment = spy(PortfolioFragment())

  @Before
  fun initMocks() {
    fragment = spy(PortfolioFragment())
  }

  @Test
  fun testFragmentUpdate() {
    attachFragmentToTestActivity(fragment)
    verify(fragment, times(2)).update()
  }

}


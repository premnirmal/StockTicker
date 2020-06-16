package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.mock.Mocker
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.tools.Parser
import com.github.premnirmal.tickerwidget.R
import com.google.gson.JsonElement
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import java.lang.reflect.Type

/**
 * Created by premnirmal on 3/22/17.
 */
@RunWith(RobolectricTestRunner::class)
abstract class BaseUnitTest : TestCase() {

  companion object {

    /**
     * Attach a fragment to a new instance of {@link TestActivity.java}
     *
     * @param fragment Fragment to add to the activity.
     */
    fun attachFragmentToTestActivity(fragment: BaseFragment): ActivityController<TestActivity> {
      val controller = Robolectric.buildActivity(TestActivity::class.java).create()
      controller.start()
      controller.resume()
      val activity = controller.get()
      val fm = activity.supportFragmentManager
      fm.beginTransaction().add(R.id.fragment_container, fragment).commit()
      return controller
    }
  }

  private val parser = Parser()

  @Before public override fun setUp() = runBlockingTest {
    super.setUp()
    val iStocksProvider = Mocker.provide(IStocksProvider::class)
    doNothing().whenever(iStocksProvider).schedule()
    whenever(iStocksProvider.fetch()).thenReturn(FetchResult.success(ArrayList()))
    whenever(iStocksProvider.getTickers()).thenReturn(emptyList())
    whenever(iStocksProvider.addStock(any())).thenReturn(emptyList())
    whenever(iStocksProvider.lastFetched()).thenReturn("--")
    whenever(iStocksProvider.nextFetch()).thenReturn("--")
  }

  fun parseJsonFile(fileName: String): JsonElement {
    return parser.parseJsonFile(fileName)
  }

  fun <T> parseJsonFile(type: Type, fileName: String): T {
    return parser.parseJsonFile(type, fileName)
  }
}
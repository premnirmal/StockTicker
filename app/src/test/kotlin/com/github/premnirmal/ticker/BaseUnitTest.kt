package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.mock.Mocker
import com.github.premnirmal.ticker.mock.RxSchedulersOverrideRule
import com.github.premnirmal.ticker.mock.TestApplication
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.tools.Parser
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.google.gson.JsonElement
import io.reactivex.Observable
import junit.framework.TestCase
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doNothing
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ActivityController
import java.lang.reflect.Type


/**
 * Created by premnirmal on 3/22/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class)
abstract class BaseUnitTest : TestCase() {

  companion object {

    /**
     * Attach a fragment to a new instance of the given activity class.
     *
     * @param fragment Fragment to add to the activity.
     */
    fun attachFragmentToTestActivity(fragment: BaseFragment)
        : ActivityController<out BaseActivity> {
      val controller = Robolectric.buildActivity(
          ParanormalActivity::class.java).create().start().resume()
      val activity = controller.get()
      val fm = activity.supportFragmentManager
      fm.beginTransaction().add(R.id.fragment_container, fragment).commit()
      return controller
    }
  }

  @get:Rule val schedulerRule = RxSchedulersOverrideRule()
  val parser = Parser()

  @Before override public fun setUp() {
    super.setUp()
    val iStocksProvider = Mocker.provide(IStocksProvider::class.java)
    doNothing().`when`(iStocksProvider).schedule()
    `when`(iStocksProvider.fetch()).thenReturn(Observable.never())
    `when`(iStocksProvider.getStocks()).thenReturn(emptyList())
    `when`(iStocksProvider.getTickers()).thenReturn(emptyList())
    `when`(iStocksProvider.addStock(ArgumentMatchers.anyString())).thenReturn(emptyList())
    `when`(iStocksProvider.removeStock(ArgumentMatchers.anyString())).thenReturn(emptyList())
    `when`(iStocksProvider.lastFetched()).thenReturn("--")
    `when`(iStocksProvider.nextFetch()).thenReturn("--")
  }

  fun parseJsonFile(fileName: String): JsonElement {
    return parser.parseJsonFile(fileName)
  }

  fun <T> parseJsonFile(type: Type, fileName: String): T {
    return parser.parseJsonFile(type, fileName)
  }
}
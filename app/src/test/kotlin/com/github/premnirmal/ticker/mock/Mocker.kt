package com.github.premnirmal.ticker.mock

import org.junit.Assert.assertTrue
import org.mockito.Mockito.mock
import java.util.HashMap
import kotlin.reflect.KClass

object Mocker {

  private val mocks = HashMap<KClass<*>, Any>()

  fun <T : Any> setMock(clazz: KClass<T>, mock: T): T {
    mocks.put(clazz, mock as Any)
    return mock
  }

  fun <T : Any> getMock(clazz: KClass<T>): T {
    assertTrue(mocks.containsKey(clazz))
    return mocks[clazz] as T
  }

  fun <T : Any> provide(clazz: KClass<T>): T {
    if (!mocks.containsKey(clazz)) {
      val mock = mock(clazz.java)
      mocks[clazz] = mock!!
    }
    return mocks[clazz] as T
  }

  fun clearMocks() {
    mocks.clear()
  }
}
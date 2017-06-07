package com.github.premnirmal.ticker.mock

import org.junit.Assert.assertTrue
import org.mockito.Mockito.mock
import java.util.HashMap

object Mocker {

  private val mocks = HashMap<Class<*>, Any>()

  fun <T> setMock(clazz: Class<T>, mock: T): T {
    mocks.put(clazz, mock as Any)
    return mock
  }

  fun <T> getMock(clazz: Class<T>): T {
    assertTrue(mocks.containsKey(clazz))
    return mocks[clazz] as T
  }

  fun <T> provide(clazz: Class<T>): T {
    if (!mocks.containsKey(clazz)) {
      val mock = mock(clazz)
      mocks.put(clazz, mock!!)
    }
    return mocks[clazz] as T
  }

  fun clearMocks() {
    mocks.clear()
  }
}
package com.github.premnirmal.ticker.mock

import org.junit.Assert.assertTrue
import org.mockito.Mockito
import java.util.HashMap

object Mocker {

  private val sMockMap = HashMap<Class<*>, Any>()

  operator fun <T> set(clazz: Class<T>, mock: T): T {
    sMockMap.put(clazz, mock!!)
    return mock
  }

  operator fun <T> get(clazz: Class<T>): T {
    assertTrue(sMockMap.containsKey(clazz))
    return sMockMap[clazz] as T
  }

  fun <T> provide(clazz: Class<T>): T {
    if (!sMockMap.containsKey(clazz)) {
      val mock = Mockito.mock(clazz)
      sMockMap.put(clazz, mock!!)
    }
    return sMockMap[clazz] as T
  }

  fun clear() {
    sMockMap.clear()
  }
}// Private constructor to prevent instantiation.
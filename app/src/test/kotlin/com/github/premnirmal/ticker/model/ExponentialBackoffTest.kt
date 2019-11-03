package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.BaseUnitTest
import org.junit.Test
import kotlin.math.pow

class ExponentialBackoffTest : BaseUnitTest() {

  private val exponentialBackoff = ExponentialBackoff()

  @Test fun testBackoffValue() {
    // Test backoff attempts 1-5
    for (count in 1..5) {
      assertEquals(exponentialBackoff.baseMs * exponentialBackoff.backoffFactor.toDouble().pow(
          count.toDouble()).toLong(), exponentialBackoff.getBackoffDurationMs(count))
    }
    // Test capped backoff
    assertEquals(exponentialBackoff.capMs, exponentialBackoff.getBackoffDurationMs(10))
    assertEquals(exponentialBackoff.capMs, exponentialBackoff.getBackoffDurationMs(20))
  }

}
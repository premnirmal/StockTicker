package com.github.premnirmal.ticker.components

import com.github.premnirmal.ticker.concurrency.ApplicationScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.filter
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.launch

/**
 * Created by premnirmal on 2/26/16.
 *
 * The ghetto event bus!
 */
class AsyncBus {

  @ExperimentalCoroutinesApi
  val bus: BroadcastChannel<Any> = BroadcastChannel(1)

  @ExperimentalCoroutinesApi fun post(o: Any) {
    ApplicationScope.launch {
      bus.send(o)
    }
  }

  @ExperimentalCoroutinesApi
  inline fun <reified T> asChannel(): ReceiveChannel<T> {
    return bus.openSubscription()
        .filter { it is T }
        .map { it as T }
  }
}

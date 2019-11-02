package com.github.premnirmal.ticker.components

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.filter
import kotlinx.coroutines.channels.map

/**
 * Created by premnirmal on 2/26/16.
 *
 * The ghetto event bus!
 */
class RxBus {

  @ExperimentalCoroutinesApi val bus: BroadcastChannel<Any> = BroadcastChannel(1)

  fun post(o: Any) {
//    ApplicationScope.launch {
//      bus.send(o)
//    }
//    flow {  }
  }

  fun <T> forEventType(clazz: Class<T>) {

  }

  inline fun <reified T> asChannel(): ReceiveChannel<T> {
    return bus.openSubscription().filter{ it is T }.map { it as T }
  }
}

package com.github.premnirmal.ticker.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Created by premnirmal on 2/26/16.
 *
 * The ghetto event bus!
 */
class AsyncBus: CoroutineScope {

  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main

  @ExperimentalCoroutinesApi
  val bus = BroadcastChannel<Any>(1)

  @ExperimentalCoroutinesApi
  fun send(o: Any) {
    launch {
      delay(2000)
      bus.send(o)
    }
  }

  @FlowPreview
  @ExperimentalCoroutinesApi
  inline fun <reified T> receive(): Flow<T> {
    return bus.openSubscription()
        .consumeAsFlow()
        .filter { it is T }
        .map { it as T }
  }
}

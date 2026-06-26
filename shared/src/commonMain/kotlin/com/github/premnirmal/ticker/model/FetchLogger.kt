package com.github.premnirmal.ticker.model

/**
 * Platform-neutral contract for recording diagnostic fetch events (a quote refresh starting,
 * succeeding or failing, the source that triggered it, etc.) into the persisted fetch log.
 *
 * This is the shared "fetch logger" of the multiplatform persistence story: it mirrors the existing
 * [RefreshScheduler] / [IStocksProvider] / [com.github.premnirmal.ticker.repo.QuoteStorage] splits —
 * the common contract lives in `commonMain`, the concrete sink is platform specific. On Android this
 * is implemented by `FetchEventLogger`, which persists each entry through the Room-backed
 * `StocksStorage.addFetchLog` (a platform-typed operation that stays on the concrete implementation)
 * and reports failures through `Timber`; the iOS app will provide its own implementation once it
 * exists.
 */
interface FetchLogger {

  /**
   * Records a fetch event. [source] identifies the component that triggered the fetch (e.g. the
   * provider, a widget receiver or the background worker), [event] is the event name, and [detail]
   * is free-form context (it may be truncated by the implementation).
   */
  fun log(source: String, event: String, detail: String)
}

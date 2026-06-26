package com.github.premnirmal.ticker.analytics

import com.github.premnirmal.ticker.components.AppLogger

/**
 * Platform sink the iOS app implements to forward analytics events to a backend (e.g. the Firebase
 * iOS SDK), or a no-op for the FOSS build. This is the iOS analogue of the per-flavor Android
 * `AnalyticsImpl`. Defaults to [NoopAnalyticsSink] so the dependency graph resolves before the
 * app wires a real sink.
 */
interface AnalyticsSink {
    fun trackScreenView(screenName: String) {}
    fun trackClickEvent(event: ClickEvent) {}
    fun trackGeneralEvent(event: GeneralEvent) {}
}

/** No-op [AnalyticsSink] used until the iOS app provides a real (e.g. Firebase) sink. */
object NoopAnalyticsSink : AnalyticsSink

/**
 * iOS analytics implementation of the shared [Analytics] contract over the shared [AnalyticsEvent]
 * model.
 *
 * It is the iOS counterpart of Android's per-flavor `AnalyticsImpl`: the platform-neutral
 * `GeneralEvent`/`ClickEvent` value types and the [Analytics] interface are shared, while the
 * platform reporting is delegated to an [AnalyticsSink] (the iOS app forwards to Firebase, or no-ops
 * for FOSS). Every event is also mirrored through the multiplatform [AppLogger] for diagnostics,
 * mirroring the debug logging the Android implementation performs.
 */
class AnalyticsImpl(
    private val sink: AnalyticsSink = NoopAnalyticsSink
) : Analytics {

    override fun trackScreenView(screenName: String) {
        AppLogger.d("Analytics screen_view: $screenName")
        sink.trackScreenView(screenName)
    }

    override fun trackClickEvent(event: ClickEvent) {
        AppLogger.d("Analytics click: ${event.name} ${event.properties}")
        sink.trackClickEvent(event)
    }

    override fun trackGeneralEvent(event: GeneralEvent) {
        AppLogger.d("Analytics event: ${event.name} ${event.properties}")
        sink.trackGeneralEvent(event)
    }
}

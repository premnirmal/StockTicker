package com.github.premnirmal.ticker.analytics

import com.github.premnirmal.ticker.components.AppLogger

/**
 * Platform sink the iOS app implements to forward analytics events to a backend (e.g. the Firebase
 * iOS SDK), or a no-op for the FOSS build. This is the iOS analogue of the per-flavor Android
 * `AnalyticsImpl`. Defaults to [NoopIosAnalyticsSink] so the dependency graph resolves before the
 * app wires a real sink.
 */
interface IosAnalyticsSink {
    fun trackScreenView(screenName: String) {}
    fun trackClickEvent(event: ClickEvent) {}
    fun trackGeneralEvent(event: GeneralEvent) {}
}

/** No-op [IosAnalyticsSink] used until the iOS app provides a real (e.g. Firebase) sink. */
object NoopIosAnalyticsSink : IosAnalyticsSink

/**
 * iOS analytics implementation over the shared [AnalyticsEvent] model.
 *
 * It is the iOS counterpart of Android's `Analytics`/`AnalyticsImpl`: the platform-neutral
 * `GeneralEvent`/`ClickEvent` value types are shared, while the platform reporting is delegated to
 * an [IosAnalyticsSink] (the iOS app forwards to Firebase, or no-ops for FOSS). Every event is also
 * mirrored through the multiplatform [AppLogger] for diagnostics, mirroring the debug logging the
 * Android implementation performs. Unlike Android, `trackScreenView` takes a plain screen name
 * (there is no `android.app.Activity` on iOS).
 */
class IosAnalytics(
    private val sink: IosAnalyticsSink = NoopIosAnalyticsSink
) {

    fun trackScreenView(screenName: String) {
        AppLogger.d("Analytics screen_view: $screenName")
        sink.trackScreenView(screenName)
    }

    fun trackClickEvent(event: ClickEvent) {
        AppLogger.d("Analytics click: ${event.name} ${event.properties}")
        sink.trackClickEvent(event)
    }

    fun trackGeneralEvent(event: GeneralEvent) {
        AppLogger.d("Analytics event: ${event.name} ${event.properties}")
        sink.trackGeneralEvent(event)
    }
}

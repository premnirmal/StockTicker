import Foundation
import Shared
#if canImport(FirebaseAnalytics)
import FirebaseAnalytics
#endif

/// Platform implementation of the shared `IosAnalyticsSink`. The shared `IosAnalytics` builds the
/// platform-neutral `AnalyticsEvent`s (the same model Android logs to Firebase) and forwards them
/// here; this maps them onto the iOS analytics backend.
///
/// Firebase is optional — when the FirebaseAnalytics SDK is not linked the events are simply logged,
/// mirroring the Android `dev` flavour's no-op analytics.
final class StockTickerAnalyticsSink: IosAnalyticsSink {

    func trackScreenView(screenName: String) {
        #if canImport(FirebaseAnalytics)
        Analytics.logEvent(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: screenName
        ])
        #else
        NSLog("[Analytics] screen_view: %@", screenName)
        #endif
    }

    func trackClickEvent(event: ClickEvent) {
        logEvent(name: event.name, properties: event.properties)
    }

    func trackGeneralEvent(event: GeneralEvent) {
        logEvent(name: event.name, properties: event.properties)
    }

    private func logEvent(name: String, properties: [String: String]) {
        #if canImport(FirebaseAnalytics)
        Analytics.logEvent(name, parameters: properties)
        #else
        NSLog("[Analytics] %@: %@", name, properties.description)
        #endif
    }
}

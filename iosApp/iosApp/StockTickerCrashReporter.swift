import Foundation
import Shared
#if canImport(FirebaseCrashlytics)
import FirebaseCrashlytics
#endif

/// Platform implementation of the shared `CrashReporter`. The shared `AppLogger` forwards every error
/// and warning here; this records errors as Firebase Crashlytics non-fatals and warnings as
/// breadcrumbs when the Crashlytics SDK is linked.
///
/// Crashlytics already auto-captures real crashes (native signals and uncaught Kotlin exceptions);
/// this sink additionally surfaces shared errors that are *caught and logged* rather than crashing.
/// When the SDK is not linked it is a no-op, mirroring the Android FOSS/dev flavours.
final class StockTickerCrashReporter: CrashReporter {

    func recordError(throwable: KotlinThrowable?, message: String?) {
        #if canImport(FirebaseCrashlytics)
        let description = [message, throwable?.message]
            .compactMap { $0 }
            .joined(separator: ": ")
        let error = NSError(
            domain: "KotlinError",
            code: 0,
            userInfo: [NSLocalizedDescriptionKey: description.isEmpty ? "Unknown error" : description]
        )
        Crashlytics.crashlytics().record(error: error)
        #endif
    }

    func log(message: String) {
        #if canImport(FirebaseCrashlytics)
        Crashlytics.crashlytics().log(message)
        #endif
    }
}

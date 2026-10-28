package com.github.premnirmal.ticker.components

/**
 * Platform sink the iOS app implements to forward shared [AppLogger] errors/warnings to a crash
 * reporting backend (e.g. the Firebase Crashlytics iOS SDK) as non-fatal records / breadcrumbs. This
 * is the iOS analogue of Android's Timber-to-Crashlytics tree.
 *
 * Crashlytics auto-captures actual crashes (native signals and uncaught Kotlin exceptions), but
 * errors that the shared code *catches and logs* never reach it on their own — this sink bridges
 * those. Defaults to [NoopCrashReporter] (the FOSS build, or before the app wires a real reporter) so
 * the shared logger never depends on a backend being present.
 */
interface CrashReporter {
    /** Records a non-fatal [throwable] (with optional [message]) as a Crashlytics non-fatal report. */
    fun recordError(throwable: Throwable?, message: String?) {}

    /** Adds a breadcrumb [message] to the next crash / non-fatal report. */
    fun log(message: String) {}
}

/** No-op [CrashReporter] used until the iOS app provides a real (e.g. Firebase Crashlytics) one. */
object NoopCrashReporter : CrashReporter

/**
 * Mutable holder for the active [CrashReporter]. The iOS app installs its reporter at launch (via
 * `initKoinIos`); [AppLogger]'s iOS sink forwards errors/warnings here.
 *
 * A plain holder (rather than a Koin lookup) is used deliberately: [AppLogger] is invoked from inside
 * the iOS Koin module's coroutine `CoroutineExceptionHandler`, so resolving the reporter through Koin
 * would be re-entrant.
 */
object IosCrashReporter {
    var reporter: CrashReporter = NoopCrashReporter
}

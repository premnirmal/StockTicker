package com.github.premnirmal.ticker.analytics

/**
 * Platform-neutral analytics event model: an event [name] plus an accumulating map of string
 * [properties].
 *
 * This is the shared "analytics event" of the multiplatform DI/platform-infra story: it mirrors the
 * other Phase 2 splits — the platform-neutral value types live in `commonMain`, while the platform
 * sink stays platform specific. The `Analytics` interface (whose `trackScreenView` takes an
 * `android.app.Activity`) and `GeneralProperties` (which depends on the Android `WidgetDataProvider`
 * /`StocksProvider`) stay on Android, and the per-flavor `AnalyticsImpl` reports these events through
 * Firebase (prod) or no-ops (purefoss/dev); the iOS app will provide its own sink once it exists.
 */
sealed class AnalyticsEvent(val name: String) {

    val properties: Map<String, String>
        get() = _properties
    private val _properties = HashMap<String, String>()

    open fun addProperty(key: String, value: String) = apply {
        _properties[key] = value
    }
}

class GeneralEvent(name: String) : AnalyticsEvent(name) {
    override fun addProperty(key: String, value: String) = apply {
        super.addProperty(key, value)
    }
}

class ClickEvent(name: String) : AnalyticsEvent(name) {
    override fun addProperty(key: String, value: String) = apply {
        super.addProperty(key, value)
    }
}

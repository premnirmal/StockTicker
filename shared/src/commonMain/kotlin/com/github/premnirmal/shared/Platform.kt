package com.github.premnirmal.shared

/**
 * Minimal platform abstraction used to verify the Kotlin Multiplatform setup and
 * to expose a symbol from the shared framework consumed by the iOS app.
 */
interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

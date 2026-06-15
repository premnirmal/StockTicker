package com.github.premnirmal.ticker.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Lenient JSON configuration shared across the multiplatform networking layer. Mirrors the
 * settings previously used by the Android-only Retrofit/`NetworkModule` setup.
 */
internal val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
    prettyPrint = true
}

/**
 * Creates a Ktor [HttpClient] configured with [ContentNegotiation] backed by [ApiJson]. The HTTP
 * engine is picked up from the classpath of the consuming platform (OkHttp on Android, Darwin on
 * iOS), so no explicit engine is specified here.
 */
fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(ApiJson)
    }
}

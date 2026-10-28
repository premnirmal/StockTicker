package com.github.premnirmal.shared

/**
 * Multiplatform abstraction over Android's `Parcelable` / `@Parcelize`.
 *
 * Shared models that genuinely need to be parceled (e.g. passed through an
 * `Intent`/`Bundle` on Android) implement [CommonParcelable] and are annotated with
 * [CommonParcelize]. On Android these are `typealias`-ed to `android.os.Parcelable` and
 * `kotlinx.parcelize.Parcelize`, so the `kotlin-parcelize` compiler plugin generates the
 * real `Parcelable` implementation. On non-Android targets [CommonParcelable] is an empty
 * marker interface and [CommonParcelize] has no actual (it is an optional expectation).
 */
expect interface CommonParcelable

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class CommonParcelize()

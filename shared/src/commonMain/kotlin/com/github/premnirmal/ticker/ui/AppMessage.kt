package com.github.premnirmal.ticker.ui

/**
 * Platform-neutral in-app message model shared by Android and iOS. The Android `AppMessaging`
 * dispatcher (which needs a `Context` to resolve string resources) stays in `:androidApp` and emits
 * these messages; the shared bottom-sheet UI renders [BottomSheetMessage].
 */
sealed class AppMessage(
    val title: String,
    val message: String,
) {
    class BottomSheetMessage(
        title: String,
        message: String,
    ) : AppMessage(title, message)

    class BannerMessage(
        title: String,
        message: String,
        onClick: (() -> Unit)? = null,
    ) : AppMessage(title, message)
}

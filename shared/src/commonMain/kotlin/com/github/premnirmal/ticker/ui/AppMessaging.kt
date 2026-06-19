package com.github.premnirmal.ticker.ui

import com.github.premnirmal.ticker.ui.AppMessage.BannerMessage
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage
import kotlinx.coroutines.flow.Flow

/**
 * Platform-neutral messaging contract that shared presentation logic (Phase 3 ViewModels) can use
 * to surface snackbars, banners and bottom sheets without depending on any Android/Compose type.
 * The Compose-backed Android implementation lives in `:app` (`ComposeAppMessaging`).
 */
interface AppMessaging {

    val bottomSheets: Flow<BottomSheetMessage>

    val banners: Flow<BannerMessage>

    fun sendSnackbar(message: String)

    fun sendBanner(
        title: String,
        message: String,
        onClick: (() -> Unit)? = null,
    )

    fun sendBottomSheet(
        title: String,
        message: String,
    )
}

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

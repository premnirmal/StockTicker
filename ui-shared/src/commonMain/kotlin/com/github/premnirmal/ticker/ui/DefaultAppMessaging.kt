package com.github.premnirmal.ticker.ui

import androidx.compose.material3.SnackbarHostState
import com.github.premnirmal.ticker.ui.AppMessage.BannerMessage
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Compose-backed implementation of the shared [AppMessaging] contract. It owns the multiplatform
 * `material3` [SnackbarHostState] consumed by the Compose UI plus the in-memory message queue that
 * drives banners/bottom sheets, all built from multiplatform `material3`/coroutines APIs, so it
 * lives in `:shared` `commonMain` and every platform can reuse it. Platform-specific extras (e.g.
 * Android string-resource overloads) live in a thin subclass in the platform host (`:app`'s
 * `ComposeAppMessaging`).
 */
open class DefaultAppMessaging(
    private val coroutineScope: CoroutineScope,
) : AppMessaging {

    val snackbarHostState = SnackbarHostState()

    private val messageQueue = MutableSharedFlow<AppMessage>(replay = 0, extraBufferCapacity = 100)

    override val bottomSheets: Flow<BottomSheetMessage>
        get() = messageQueue.filterIsInstance()

    override val banners: Flow<BannerMessage>
        get() = messageQueue.filterIsInstance()

    override fun sendSnackbar(
        message: String,
    ) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    override fun sendBanner(
        title: String,
        message: String,
        onClick: (() -> Unit)?,
    ) {
        coroutineScope.launch {
            messageQueue.emit(
                BannerMessage(
                    title = title,
                    message = message,
                    onClick = onClick,
                )
            )
        }
    }

    override fun sendBottomSheet(
        title: String,
        message: String,
    ) {
        coroutineScope.launch {
            messageQueue.emit(BottomSheetMessage(title = title, message = message))
        }
    }
}

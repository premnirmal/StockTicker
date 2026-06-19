package com.github.premnirmal.ticker.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import com.github.premnirmal.ticker.ui.AppMessage.BannerMessage
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Compose/Android backed implementation of the shared [AppMessaging] contract. It owns the
 * [SnackbarHostState] consumed by the Compose UI and resolves Android string resources, while
 * delegating the platform-neutral messaging to the shared interface.
 */
class ComposeAppMessaging constructor(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) : AppMessaging {

    val snackbarHostState = SnackbarHostState()

    override val bottomSheets: Flow<BottomSheetMessage>
        get() = _messageQueue.filterIsInstance(BottomSheetMessage::class)

    override val banners: Flow<BannerMessage>
        get() = _messageQueue.filterIsInstance(BannerMessage::class)

    private val _messageQueue = MutableSharedFlow<AppMessage>(replay = 0, extraBufferCapacity = 100)

    override fun sendSnackbar(
        message: String,
    ) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message
            )
        }
    }

    fun sendSnackbar(
        message: Int,
    ) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                context.getString(message)
            )
        }
    }

    override fun sendBanner(
        title: String,
        message: String,
        onClick: (() -> Unit)?,
    ) {
        coroutineScope.launch {
            _messageQueue.emit(
                BannerMessage(
                    title = title,
                    message = message,
                    onClick = onClick,
                )
            )
        }
    }

    fun sendBanner(
        title: Int,
        message: Int,
        onClick: (() -> Unit)? = null,
    ) {
        sendBanner(
            title = context.getString(title),
            message = context.getString(message),
            onClick = onClick,
        )
    }

    override fun sendBottomSheet(
        title: String,
        message: String,
    ) {
        coroutineScope.launch {
            _messageQueue.emit(BottomSheetMessage(title = title, message = message))
        }
    }

    fun sendBottomSheet(
        title: Int,
        message: String,
    ) {
        sendBottomSheet(
            title = context.getString(title),
            message = message,
        )
    }
}

val LocalAppMessaging = staticCompositionLocalOf<ComposeAppMessaging> {
    error("No AppMessaging sender provided")
}

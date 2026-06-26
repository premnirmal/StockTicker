package com.github.premnirmal.ticker.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

class AppMessaging constructor(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) {

    val snackbarHostState = SnackbarHostState()

    val bottomSheets: Flow<BottomSheetMessage>
        get() = _messageQueue.filterIsInstance(BottomSheetMessage::class)

    val banners: Flow<AppMessage.BannerMessage>
        get() = _messageQueue.filterIsInstance(AppMessage.BannerMessage::class)

    private val _messageQueue = MutableSharedFlow<AppMessage>(replay = 0, extraBufferCapacity = 100)

    fun sendSnackbar(
        message: Int,
    ) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                context.getString(message)
            )
        }
    }

    fun sendSnackbar(
        message: String,
    ) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message
            )
        }
    }

    fun sendBanner(
        title: Int,
        message: Int,
        onClick: (() -> Unit)? = null,
    ) {
        coroutineScope.launch {
            _messageQueue.emit(
                AppMessage.BannerMessage(
                    title = context.getString(title),
                    message = context.getString(message),
                    onClick = onClick,
                )
            )
        }
    }

    fun sendBottomSheet(
        title: Int,
        message: String,
    ) {
        coroutineScope.launch {
            _messageQueue.emit(
                BottomSheetMessage(
                    title = context.getString(title),
                    message = message,
                )
            )
        }
    }

    fun sendBottomSheet(
        title: String,
        message: String,
    ) {
        coroutineScope.launch {
            _messageQueue.emit(BottomSheetMessage(title = title, message = message))
        }
    }
}

val LocalAppMessaging = staticCompositionLocalOf<AppMessaging> {
    error("No AppMessaging sender provided")
}

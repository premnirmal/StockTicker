package com.github.premnirmal.ticker.ui

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage
import com.github.premnirmal.ticker.ui.AppMessage.SnackbarMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppMessaging @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
) {

    val snackbars: Flow<SnackbarMessage>
        get() = _messageQueue.filterIsInstance(SnackbarMessage::class)

    val bottomSheets: Flow<BottomSheetMessage>
        get() = _messageQueue.filterIsInstance(BottomSheetMessage::class)
    private val _messageQueue = MutableSharedFlow<AppMessage>(replay = 0, extraBufferCapacity = 100)

    fun sendSnackbar(
        title: Int,
        message: Int,
    ) {
        coroutineScope.launch {
            delay(MESSAGE_EMIT_DELAY)
            _messageQueue.emit(SnackbarMessage(context.getString(title), context.getString(message)))
        }
    }

    fun sendSnackbar(
        message: Int,
    ) {
        coroutineScope.launch {
            delay(MESSAGE_EMIT_DELAY)
            _messageQueue.emit(SnackbarMessage("", context.getString(message)))
        }
    }

    fun sendSnackbar(
        message: String,
    ) {
        coroutineScope.launch {
            delay(MESSAGE_EMIT_DELAY)
            _messageQueue.emit(SnackbarMessage("", message))
        }
    }

    fun sendSnackbar(
        title: String,
        message: String,
    ) {
        coroutineScope.launch {
            delay(MESSAGE_EMIT_DELAY)
            _messageQueue.emit(SnackbarMessage(title, message))
        }
    }

    fun sendBottomSheet(
        title: Int,
        message: Int,
    ) {
        coroutineScope.launch {
            delay(MESSAGE_EMIT_DELAY)
            _messageQueue.emit(
                BottomSheetMessage(
                    title = context.getString(title),
                    message = context.getString(message),
                )
            )
        }
    }

    fun sendBottomSheet(
        title: Int,
        message: String,
    ) {
        coroutineScope.launch {
            delay(MESSAGE_EMIT_DELAY)
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
            delay(MESSAGE_EMIT_DELAY)
            _messageQueue.emit(BottomSheetMessage(title = title, message = message))
        }
    }

    companion object {
        private const val MESSAGE_EMIT_DELAY = 300L
    }
}

sealed class AppMessage(
    val title: String,
    val message: String,
) {
    class SnackbarMessage(
        title: String,
        message: String,
        val isError: Boolean = false,
    ) : AppMessage(title, message)

    class BottomSheetMessage(
        title: String,
        message: String,
    ) : AppMessage(title, message)
}

val LocalAppMessaging = staticCompositionLocalOf<AppMessaging> {
    error("No AppMessaging sender provided")
}

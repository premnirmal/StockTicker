package com.github.premnirmal.ticker.ui

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage
import com.github.premnirmal.ticker.ui.AppMessage.SnackbarMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
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

    val snackbarQueue: Flow<SnackbarMessage>
        get() = _messageQueue.filterIsInstance(SnackbarMessage::class)

    val bottomsheetQueue: Flow<BottomSheetMessage>
        get() = _messageQueue.filterIsInstance(BottomSheetMessage::class)

    val messageQueue: Flow<AppMessage>
        get() = _messageQueue
    private val _messageQueue = MutableSharedFlow<AppMessage>(replay = 0)

    fun sendSnackbar(
        title: Int,
        message: Int,
    ) {
        coroutineScope.launch {
            _messageQueue.emit(SnackbarMessage(context.getString(title), context.getString(message)))
        }
    }

    fun sendSnackbar(
        message: Int,
    ) {
        coroutineScope.launch {
            _messageQueue.emit(SnackbarMessage("", context.getString(message)))
        }
    }

    fun sendSnackbar(
        message: String,
    ) {
        coroutineScope.launch {
            _messageQueue.emit(SnackbarMessage("", message))
        }
    }

    fun sendSnackbar(
        title: String,
        message: String,
    ) {
        coroutineScope.launch {
            _messageQueue.emit(SnackbarMessage(title, message))
        }
    }

    fun sendBottomSheet(
        title: Int,
        message: Int,
    ) {
        coroutineScope.launch {
            _messageQueue.emit(
                BottomSheetMessage(
                    context.getString(title),
                    context.getString(message),
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
                    context.getString(title),
                    message,
                )
            )
        }
    }

    fun sendBottomSheet(
        title: String,
        message: String,
        dismissText: String
    ) {
        coroutineScope.launch {
            _messageQueue.emit(BottomSheetMessage(title, message))
        }
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

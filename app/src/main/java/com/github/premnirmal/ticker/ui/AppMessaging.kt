package com.github.premnirmal.ticker.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage
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

    val snackbarHostState = SnackbarHostState()
    val bottomSheets: Flow<BottomSheetMessage>
        get() = _messageQueue.filterIsInstance(BottomSheetMessage::class)
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

sealed class AppMessage(
    val title: String,
    val message: String,
) {
    class BottomSheetMessage(
        title: String,
        message: String,
    ) : AppMessage(title, message)
}

val LocalAppMessaging = staticCompositionLocalOf<AppMessaging> {
    error("No AppMessaging sender provided")
}

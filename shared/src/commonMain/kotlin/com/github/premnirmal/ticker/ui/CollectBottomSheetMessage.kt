package com.github.premnirmal.ticker.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive

/**
 * Collects [bottomSheets] messages, queues them and shows one bottom sheet at a time. The message
 * source is hoisted as a [Flow] parameter so this stays free of the Android-only `AppMessaging`
 * dispatcher; the platform host supplies `LocalAppMessaging.current.bottomSheets`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectBottomSheetMessage(bottomSheets: Flow<BottomSheetMessage>) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val bottomSheetMessageQueue = remember {
        ArrayDeque<BottomSheetMessage>()
    }
    var bottomSheetMessage: BottomSheetMessage? by remember {
        mutableStateOf(null)
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            bottomSheets.collect { message ->
                bottomSheetMessageQueue.add(message)
            }
        }
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                delay(600L)
                if (bottomSheetMessage == null) {
                    bottomSheetMessage = bottomSheetMessageQueue.removeFirstOrNull()
                }
            }
        }
    }

    bottomSheetMessage?.let {
        BottomSheetWithMessage(message = it) {
            bottomSheetMessage = null
        }
    }
}

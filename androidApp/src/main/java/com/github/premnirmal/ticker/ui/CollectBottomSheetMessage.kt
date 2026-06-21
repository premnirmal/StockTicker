package com.github.premnirmal.ticker.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.LinkedList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifecycleOwner.CollectBottomSheetMessage() {
    val appMessaging = LocalAppMessaging.current
    val bottomSheetMessageQueue = remember {
        LinkedList<BottomSheetMessage>()
    }
    var bottomSheetMessage: BottomSheetMessage? by remember {
        mutableStateOf(null)
    }

    LaunchedEffect(Unit) {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            appMessaging.bottomSheets.collect { message ->
                bottomSheetMessageQueue.add(message)
            }
        }
    }

    LaunchedEffect(Unit) {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                delay(600L)
                if (bottomSheetMessage == null) {
                    bottomSheetMessage = bottomSheetMessageQueue.poll()
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

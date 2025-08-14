package com.github.premnirmal.ticker.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifecycleOwner.CollectBottomSheetMessage() {
    val appMessaging = LocalAppMessaging.current
    var bottomSheetMessage: BottomSheetMessage? by remember {
        mutableStateOf(null)
    }

    bottomSheetMessage?.let {
        ModalBottomSheet(
            onDismissRequest = {
                bottomSheetMessage = null
            }
        ) {
            ModalBottomSheetWithMessage(it)
        }
    }

    LaunchedEffect(Unit) {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            appMessaging.bottomsheetQueue.collect { message ->
                bottomSheetMessage = message
            }
        }
    }
}

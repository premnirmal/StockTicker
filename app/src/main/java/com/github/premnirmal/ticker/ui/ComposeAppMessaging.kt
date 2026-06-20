package com.github.premnirmal.ticker.ui

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope

/**
 * Android host for the shared [DefaultAppMessaging]: adds the Android string-resource overloads
 * (resolved via [Context]) on top of the shared snackbar/banner/bottom-sheet messaging core.
 */
class ComposeAppMessaging constructor(
    private val context: Context,
    coroutineScope: CoroutineScope,
) : DefaultAppMessaging(coroutineScope) {

    fun sendSnackbar(
        message: Int,
    ) {
        sendSnackbar(context.getString(message))
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

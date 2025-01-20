package com.github.premnirmal.ticker.ui

import android.annotation.SuppressLint
import android.content.Context
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage
import com.github.premnirmal.ticker.ui.AppMessage.SnackbarMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object AppMessaging {

  lateinit var context: Context

  val messageQueue: Flow<AppMessage>
    get() = _messageQueue
  private val _messageQueue = MutableSharedFlow<AppMessage>(replay = 0)

  init {
    Injector.appComponent().inject(this)
  }

  fun sendSnackbar(
    title: Int,
    message: Int
  ) {
    GlobalScope.launch {
      _messageQueue.emit(SnackbarMessage(context.getString(title), context.getString(message)))
    }
  }

  fun sendSnackbar(
    title: String,
    message: String
  ) {
    GlobalScope.launch {
      _messageQueue.emit(SnackbarMessage(title, message))
    }
  }

  fun sendBottomSheet(
    title: Int,
    message: Int,
    dismissText: Int
  ) {
    GlobalScope.launch {
      _messageQueue.emit(
          BottomSheetMessage(
              context.getString(title), context.getString(message), context.getString(dismissText)
          )
      )
    }
  }

  fun sendBottomSheet(
    title: Int,
    message: String,
    dismissText: Int
  ) {
    GlobalScope.launch {
      _messageQueue.emit(
          BottomSheetMessage(
              context.getString(title), message, context.getString(dismissText)
          )
      )
    }
  }

  fun sendBottomSheet(
    title: String,
    message: String,
    dismissText: String
  ) {
    GlobalScope.launch {
      _messageQueue.emit(BottomSheetMessage(title, message, dismissText))
    }
  }
}

sealed class AppMessage(
  val title: String,
  val message: String
) {
  class SnackbarMessage(
    title: String,
    message: String
  ) : AppMessage(title, message)

  class BottomSheetMessage(
    title: String,
    message: String,
    val dismissText: String
  ) : AppMessage(title, message)
}
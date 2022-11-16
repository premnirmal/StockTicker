package com.github.premnirmal.ticker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.window.layout.DisplayFeature
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.TwoPane
import com.google.accompanist.adaptive.TwoPaneStrategy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

@Composable
fun ListDetail(
  isDetailOpen: Boolean,
  setIsDetailOpen: (Boolean) -> Unit,
  showListAndDetail: Boolean,
  detailKey: Any,
  list: @Composable (isDetailVisible: Boolean) -> Unit,
  detail: @Composable (isListVisible: Boolean) -> Unit,
  twoPaneStrategy: TwoPaneStrategy,
  displayFeatures: List<DisplayFeature>,
  modifier: Modifier = Modifier,
) {
  val currentIsDetailOpen by rememberUpdatedState(isDetailOpen)
  val currentShowListAndDetail by rememberUpdatedState(showListAndDetail)
  val currentDetailKey by rememberUpdatedState(detailKey)

  // Determine whether to show the list and/or the detail.
  // This is a function of current app state, and the width size class.
  val showList by remember {
    derivedStateOf {
      currentShowListAndDetail || !currentIsDetailOpen
    }
  }
  val showDetail by remember {
    derivedStateOf {
      currentShowListAndDetail || currentIsDetailOpen
    }
  }
  // Validity check: we should always be showing something
  check(showList || showDetail)

  val listSaveableStateHolder = rememberSaveableStateHolder()
  val detailSaveableStateHolder = rememberSaveableStateHolder()

  val start = remember {
    movableContentOf {
      // Set up a SaveableStateProvider so the list state will be preserved even while it
      // is hidden if the detail is showing instead.
      listSaveableStateHolder.SaveableStateProvider(0) {
        Box {
          list(showDetail)
        }
      }
    }
  }

  val end = remember {
    movableContentOf {
      // Set up a SaveableStateProvider against the selected word index to save detail
      // state while switching between details.
      // If this behavior isn't desired, this can be replaced with a key on the
      // selectedWordIndex.
      detailSaveableStateHolder.SaveableStateProvider(currentDetailKey) {
        Box {
          detail(showList)
        }
      }

      // If showing just the detail, allow a back press to hide the detail to return to
      // the list.
      if (!showList) {
        BackHandler {
          setIsDetailOpen(false)
        }
      }
    }
  }

  Box(modifier = modifier) {
    if (showList && showDetail) {
      TwoPane(
          first = {
            start()
          },
          second = {
            end()
          },
          strategy = twoPaneStrategy,
          displayFeatures = displayFeatures,
          foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
          modifier = Modifier.fillMaxSize(),
      )
    } else if (showList) {
      start()
    } else {
      end()
    }
  }
}

fun Modifier.userInteractionNotification(onInteracted: () -> Unit): Modifier {
  return pointerInput(onInteracted) {
    val currentContext = currentCoroutineContext()
    awaitPointerEventScope {
      while (currentContext.isActive) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        // if user taps (down) or scrolls - consider it an interaction signal
        if (
            event.type == PointerEventType.Press || event.type == PointerEventType.Scroll
        ) {
          onInteracted.invoke()
        }
      }
    }
  }
}

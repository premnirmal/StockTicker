package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier

/**
 * Multiplatform list/detail layout shared by Android and iOS.
 *
 * When [showListAndDetail] is true the [list] and [detail] are placed side by side in a weighted
 * [Row] (the cross-platform replacement for Accompanist's `TwoPane`, which is Android-only and
 * fold-aware; folds are irrelevant on iPad). Otherwise a single pane is shown — the [list] until a
 * detail is opened ([isDetailOpen]), then the [detail], with a system back press returning to the
 * list via [setIsDetailOpen].
 *
 * The [list] and [detail] are wrapped in [movableContentOf] + `SaveableStateProvider` so their state
 * is preserved across single/dual-pane transitions (e.g. an iPad rotation or Split View resize) and
 * while one pane is hidden.
 *
 * System back handling is platform-specific (Android's `OnBackPressedDispatcher` vs the iOS
 * edge-swipe gesture), so it is supplied by the caller via [backHandler]: when only the detail is
 * shown, the layout invokes `backHandler { setIsDetailOpen(false) }` so a back gesture returns to the
 * list. In dual-pane mode no back handler is registered.
 *
 * @param splitFraction fraction of the width given to the [list] pane in dual-pane mode (the [detail]
 *   takes the remainder); matches Android's `HorizontalTwoPaneStrategy(splitFraction)`.
 */
@Composable
fun ListDetail(
    isDetailOpen: Boolean,
    setIsDetailOpen: (Boolean) -> Unit,
    showListAndDetail: Boolean,
    detailKey: Any,
    list: @Composable (isDetailVisible: Boolean) -> Unit,
    detail: @Composable (isListVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    splitFraction: Float = 0.5f,
    backHandler: @Composable (onBack: () -> Unit) -> Unit = {},
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
            // Set up a SaveableStateProvider against the selected detail key to save detail
            // state while switching between details.
            detailSaveableStateHolder.SaveableStateProvider(currentDetailKey) {
                Box {
                    detail(showList)
                }
            }

            // If showing just the detail, allow a back press to hide the detail to return to
            // the list. The actual back-gesture registration is platform-specific and supplied by
            // the caller via [backHandler].
            if (!showList) {
                backHandler { setIsDetailOpen(false) }
            }
        }
    }

    Box(modifier = modifier) {
        if (showList && showDetail) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(splitFraction)) {
                    start()
                }
                Box(modifier = Modifier.weight(1f - splitFraction)) {
                    end()
                }
            }
        } else if (showList) {
            start()
        } else {
            end()
        }
    }
}

package com.github.premnirmal.ticker.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.LinkedList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifecycleOwner.CollectSnackbarMessage() {
    val appMessaging = LocalAppMessaging.current

    val snackbarQueue = remember {
        LinkedList<AppMessage.SnackbarMessage>()
    }
    var snackbar: AppMessage.SnackbarMessage? by remember {
        mutableStateOf(null)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            appMessaging.snackbars.collect { message ->
                snackbarQueue.add(message)
            }
        }
    }
    LaunchedEffect(Unit) {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                if (snackbar == null) {
                    snackbar = snackbarQueue.poll()
                }
                delay(600L)
            }
        }
    }

    LaunchedEffect(snackbar) {
        snackbar?.let {
            val message = if (it.title.isNotEmpty()) {
                it.title + "\n" + it.message
            } else {
                it.message
            }
            snackbarHostState.showSnackbar(message)
            snackbar = null
        }
    }

    SnackbarHost(hostState = snackbarHostState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .systemBarsPadding()
                    .align(Alignment.BottomCenter)
            ) {
                GradientSnackBar(
                    message = snackbarHostState.currentSnackbarData?.visuals?.message.orEmpty()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientSnackBar(
    message: String,
    modifier: Modifier = Modifier,
) {
    val (alphaAnim, offsetAnim) = animateFadeAndOffsetSnackBar()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .alpha(alphaAnim)
            .padding(8.dp)
            .offset(y = offsetAnim),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )
                )
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                textAlign = TextAlign.Start,
                lineHeight = 14.sp * 1.2f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun animateFadeAndOffsetSnackBar(): Pair<Float, Dp> {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(),
        label = "alpha"
    )
    val offset by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = tween(),
        label = "offset"
    )
    return alpha to offset
}

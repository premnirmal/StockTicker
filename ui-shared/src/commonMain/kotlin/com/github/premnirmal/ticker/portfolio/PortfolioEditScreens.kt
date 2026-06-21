package com.github.premnirmal.ticker.portfolio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.ui.AppTextFieldDefaultColors
import com.github.premnirmal.ticker.ui.AppTextFieldShape
import com.github.premnirmal.ticker.ui.TopBar

/**
 * The shared per-ticker "edit notes" screen body: an [imePadding] [Scaffold] with a [TopBar]
 * (back + done actions) over a single auto-focused multi-line [TextField] bound to the already-shared
 * [NotesViewModel]. It depends only on multiplatform `material3`/`foundation`/`compose.ui` APIs plus
 * the already-shared text-field styling, so it lives in `:ui-shared` `commonMain` and iOS can reuse
 * it directly. Following the established seam pattern, every Android coupling is hoisted to a parameter:
 * the labels are plain `String`s, the icons are multiplatform [Painter]s, the snackbar host is a
 * passed-in [SnackbarHostState], and navigation/result delivery is hoisted to [onBack]/[onDone] so the
 * Android `Activity` host keeps `finish()`/`setResult(...)` in `:app`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    ticker: String,
    title: String,
    addNotesLabel: String,
    doneContentDescription: String,
    backIcon: Painter,
    doneIcon: Painter,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onDone: (String) -> Unit,
) {
    var notes by remember(ticker) {
        val text = viewModel.quote?.properties?.notes ?: ""
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(text.length),
            )
        )
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopBar(
                text = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = backIcon,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            viewModel.setNotes(notes.text)
                            onDone(notes.text)
                        }
                    ) {
                        Icon(
                            painter = doneIcon,
                            contentDescription = doneContentDescription,
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val focusRequester = remember { FocusRequester() }
            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                text = ticker,
                style = MaterialTheme.typography.headlineMedium,
            )
            TextField(
                shape = AppTextFieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
                    .focusRequester(focusRequester)
                    .verticalScroll(rememberScrollState()),
                value = notes,
                label = { Text(text = addNotesLabel) },
                onValueChange = { notes = it },
                colors = AppTextFieldDefaultColors,
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}

/**
 * The shared per-ticker "edit display name" screen body: an [imePadding] [Scaffold] with a [TopBar]
 * (back + done actions) over a single auto-focused [TextField] bound to the already-shared
 * [DisplaynameViewModel]. Like [NotesScreen] it depends only on multiplatform Compose APIs and hoists
 * every Android coupling (labels, [Painter] icons, navigation/result delivery) to parameters, so it
 * lives in `:ui-shared` `commonMain` and the Android `Activity` host keeps `finish()`/`setResult(...)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaynameScreen(
    viewModel: DisplaynameViewModel,
    ticker: String,
    title: String,
    addDisplaynameLabel: String,
    doneContentDescription: String,
    backIcon: Painter,
    doneIcon: Painter,
    onBack: () -> Unit,
    onDone: (String) -> Unit,
) {
    var displayname by remember(ticker) {
        val text = viewModel.quote?.properties?.displayname ?: ""
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(text.length),
            )
        )
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopBar(
                text = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = backIcon,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            viewModel.setDisplayname(displayname.text)
                            onDone(displayname.text)
                        }
                    ) {
                        Icon(
                            painter = doneIcon,
                            contentDescription = doneContentDescription,
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val focusRequester = remember { FocusRequester() }
            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                text = ticker,
                style = MaterialTheme.typography.headlineMedium,
            )
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 16.dp)
                    .focusRequester(focusRequester)
                    .verticalScroll(rememberScrollState()),
                value = displayname,
                label = { Text(text = addDisplaynameLabel) },
                onValueChange = { displayname = it },
                colors = TextFieldDefaults.colors().copy(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}

package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.CommitsProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object WhatsNewKoin : KoinComponent {
    val commitsProvider: CommitsProvider by inject()
}

/**
 * Drives the iOS "What's new" changelog bottom sheet. It mirrors Android's
 * `HomeViewModel.showWhatsNew()`, which loads the changelog from the shared [CommitsProvider] (the
 * git history baked into the build) and presents it. On iOS the Settings "What's new" row calls
 * [show] to load the changelog and present the [WhatsNewBottomSheet].
 */
class WhatsNewController(private val commitsProvider: CommitsProvider) {

    var visible by mutableStateOf(false)
        private set
    var lines by mutableStateOf<List<String>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Loads the changelog from the shared [CommitsProvider] and presents the bottom sheet. */
    fun show() {
        val result = commitsProvider.loadWhatsNew()
        if (result.wasSuccessful) {
            lines = result.data.filter { it.isNotBlank() }
            error = null
        } else {
            lines = emptyList()
            error = "Error fetching what's new\n\n :( ${result.error.message.orEmpty()}"
        }
        visible = true
    }

    fun dismiss() {
        visible = false
    }
}

@Composable
fun rememberWhatsNewController(): WhatsNewController =
    remember { WhatsNewController(WhatsNewKoin.commitsProvider) }

/**
 * iOS "What's new" bottom sheet. A [ModalBottomSheet] listing the changelog as bullet points,
 * matching Android's bottom sheet from `HomeViewModel.showWhatsNew()`. The changelog itself is
 * produced by the shared [CommitsProvider], so both platforms display the same content. Shown by a
 * [WhatsNewController].
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WhatsNewBottomSheet(controller: WhatsNewController, versionName: String) {
    if (!controller.visible) return

    ModalBottomSheet(
        dragHandle = { BottomSheetHandle() },
        onDismissRequest = { controller.dismiss() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            Text(
                text = "What's new in v$versionName",
                style = MaterialTheme.typography.titleLarge,
            )
            val errorMessage = controller.error
            if (errorMessage != null) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(controller.lines) { line ->
                        Text(
                            text = "\u25CF $line",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomSheetHandle(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(
            Modifier.size(width = 32.dp, height = 4.dp),
        )
    }
}

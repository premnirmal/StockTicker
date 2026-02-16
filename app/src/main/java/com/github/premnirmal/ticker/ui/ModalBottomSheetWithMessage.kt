package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.ui.AppMessage.BottomSheetMessage

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BottomSheetWithMessage(
    message: BottomSheetMessage,
    onDismissRequest: () -> Unit = {},
) {
    ModalBottomSheet(
        dragHandle = {
            BottomSheetHandle()
        },
        onDismissRequest = onDismissRequest,
    ) {
        ModalBottomSheetWithMessage(message)
    }
}

@Composable
private fun ModalBottomSheetWithMessage(
    message: BottomSheetMessage
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            text = message.title,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            modifier = Modifier
                .padding(bottom = 8.dp),
            text = message.message,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BottomSheetHandle(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            Modifier.size(width = 32.dp, height = 4.dp)
        )
    }
}

@Preview
@Composable
private fun PreviewBottomSheetWithMessage() {
    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetWithMessage(
            message = BottomSheetMessage(
                title = "Title",
                message = "This is a sample message for the bottom sheet."
            ),
        )
    }
}

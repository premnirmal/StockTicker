package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModalBottomSheetWithMessage(
    message: AppMessage.BottomSheetMessage
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = message.title,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = message.message,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

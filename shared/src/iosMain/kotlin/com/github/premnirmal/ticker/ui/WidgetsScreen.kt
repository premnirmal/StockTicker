package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_widget
import org.jetbrains.compose.resources.painterResource

/**
 * iOS Widgets tab. Android configures Glance home-screen widgets in-app; on iOS widgets are added and
 * configured from the home screen through WidgetKit (a separate extension process), so there is no
 * in-app widget list to edit. This screen explains how to add the native WidgetKit widget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Widgets") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_widget),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = "Home Screen Widgets",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "Add a Stock Ticker widget from your iOS Home Screen: touch and hold the " +
                    "Home Screen, tap the + button, then search for Stock Ticker and choose a " +
                    "widget size.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "Each widget can be configured on its own: touch and hold a placed widget and " +
                    "tap Edit Widget to choose which watchlist symbols it shows and how it looks " +
                    "(sort by change, header, change amount and bold text).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

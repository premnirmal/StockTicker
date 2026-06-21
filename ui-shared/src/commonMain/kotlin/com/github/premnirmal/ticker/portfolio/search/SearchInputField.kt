package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.ui.AppTextFieldDefaultColors
import com.github.premnirmal.ticker.ui.AppTextFieldShape

/**
 * The portfolio-search input row: an [AppTextFieldShape]/[AppTextFieldDefaultColors]-styled
 * [TextField] that capitalises input to upper-case symbols and exposes a trailing clear button.
 * It depends only on multiplatform `material3`/`foundation`/`compose.ui` APIs plus the already-shared
 * text-field styling, so it lives in `:shared` `commonMain`. Its only Android couplings — the field
 * [label] and the [clearIcon] — are passed in as plain `String`/`Painter` parameters so the
 * `R.string`/`R.drawable` lookups stay at the `:app` call site.
 */
@Composable
fun SearchInputField(
    searchQuery: String,
    label: String,
    clearIcon: Painter,
    onQueryChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        val focusManager = LocalFocusManager.current
        var text by remember {
            mutableStateOf(searchQuery)
        }
        TextField(
            shape = AppTextFieldShape,
            modifier = Modifier.fillMaxWidth(),
            colors = AppTextFieldDefaultColors,
            value = text,
            onValueChange = {
                text = it
                onQueryChange(it)
            },
            label = {
                Text(label)
            },
            singleLine = true,
            keyboardActions = KeyboardActions {
                focusManager.clearFocus(force = true)
            },
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Characters),
            trailingIcon = {
                IconButton(
                    enabled = text.isNotEmpty(),
                    onClick = {
                        text = ""
                        onQueryChange("")
                        focusManager.clearFocus(force = true)
                    },
                ) {
                    Icon(
                        painter = clearIcon,
                        contentDescription = null
                    )
                }
            }
        )
    }
}

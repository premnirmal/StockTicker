package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.premnirmal.ticker.model.Range

/**
 * The quote-detail chart time-range selector: a row of `material3` `FilterChip`s for each shared
 * [Range] option. It depends only on the multiplatform `material3`/`foundation` APIs plus the
 * already-shared [Range] model, so it lives in `commonMain` and is reused by Android and iOS.
 *
 * The short range labels come from Android `R.string` resources, so they are hoisted into plain
 * `String` parameters (the call site keeps the `stringResource` lookups). The selection change is
 * hoisted into [onRangeSelected].
 */
@Composable
fun RangeSelector(
    selectedRange: Range,
    oneDayLabel: String,
    twoWeeksLabel: String,
    oneMonthLabel: String,
    threeMonthLabel: String,
    oneYearLabel: String,
    fiveYearsLabel: String,
    maxLabel: String,
    onRangeSelected: (Range) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ranges = listOf(
        Range.ONE_DAY to oneDayLabel,
        Range.TWO_WEEKS to twoWeeksLabel,
        Range.ONE_MONTH to oneMonthLabel,
        Range.THREE_MONTH to threeMonthLabel,
        Range.ONE_YEAR to oneYearLabel,
        Range.FIVE_YEARS to fiveYearsLabel,
        Range.MAX to maxLabel,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ranges.forEach { (range, label) ->
            FilterChip(
                onClick = { onRangeSelected(range) },
                // Preserves the original `:app` behaviour: the chip is styled "selected" for every
                // range except the currently-selected one (i.e. the tappable alternatives are
                // highlighted). Kept identical to avoid a behaviour change during this leaf move.
                selected = selectedRange != range,
                label = { Text(text = label) }
            )
        }
    }
}

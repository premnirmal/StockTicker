package com.github.premnirmal.ticker.portfolio

import android.icu.text.DecimalFormatSymbols
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class DecimalFormatter(
    symbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance()
) {
    private val decimalSeparator = symbols.decimalSeparator

    fun cleanup(input: String): String {
        if (input.isEmpty()) return ""
        if (input.matches("\\D".toRegex())) return ""
        if (input.matches("0+".toRegex())) return "0"
        val sb = StringBuilder()
        var hasDecimalSep = false
        for (char in input) {
            if (char.isDigit()) {
                sb.append(char)
                continue
            }
            // Accept either the locale decimal separator or '.' so that
            // pre-populated values from Float.toString() (which always uses '.')
            // work regardless of locale.
            if ((char == decimalSeparator || char == '.') && !hasDecimalSep && sb.isNotEmpty()) {
                sb.append(decimalSeparator)
                hasDecimalSep = true
            }
        }

        return sb.toString()
    }

    fun formatForVisual(input: String): String {
        val split = input.split(decimalSeparator)
        val intPart = split[0]
        val fractionPart = split.getOrNull(1)
        return if (fractionPart == null) intPart else intPart + decimalSeparator + fractionPart
    }
}

class DecimalInputVisualTransformation(
    private val decimalFormatter: DecimalFormatter
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val inputText = text.text
        val formattedNumber = decimalFormatter.formatForVisual(inputText)
        val newText = AnnotatedString(
            text = formattedNumber,
            spanStyles = text.spanStyles,
            paragraphStyles = text.paragraphStyles
        )
        // formatForVisual preserves character count (it only re-joins using the same
        // decimal separator), so using Identity is both correct and avoids the
        // broken constant-offset mapping that was crashing the TextField when the
        // field was pre-populated with an existing alert value.
        return TransformedText(newText, OffsetMapping.Identity)
    }
}

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
        if (input.matches("\\D".toRegex())) return ""
        if (input.matches("0+".toRegex())) return "0"
        val sb = StringBuilder()
        var hasDecimalSep = false
        for (char in input) {
            if (hasDecimalSep) {
                if (char.isDigit() && sb.split(decimalSeparator)[1].length < 2) {
                    sb.append(char)
                }
                continue
            }
            if (char.isDigit()) {
                sb.append(char)
                continue
            }
            if (char == decimalSeparator && sb.isNotEmpty()) {
                sb.append(char)
                hasDecimalSep = true
            }
        }

        return sb.toString()
    }

    fun formatForVisual(input: String): String {
        val split = input.split(decimalSeparator)
        val intPart = split[0]
        val fractionPart = split.getOrNull(1)?.take(2)
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
        val offsetMapping = FixedCursorOffsetMapping(
            contentLength = inputText.length,
            formattedContentLength = formattedNumber.length
        )
        return TransformedText(newText, offsetMapping)
    }
}

private class FixedCursorOffsetMapping(
    private val contentLength: Int,
    private val formattedContentLength: Int,
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int = formattedContentLength
    override fun transformedToOriginal(offset: Int): Int = contentLength
}

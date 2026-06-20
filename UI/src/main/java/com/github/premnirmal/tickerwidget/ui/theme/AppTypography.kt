package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.github.premnirmal.tickerwidget.ui.R

val Regular = FontFamily(Font(R.font.ubuntu_regular))
val Medium = FontFamily(Font(R.font.ubuntu_medium))
val Light = FontFamily(Font(R.font.ubuntu_light))
val Bold = FontFamily(Font(R.font.ubuntu_bold))
val Italic = FontFamily(Font(R.font.ubuntu_italic))
val Raleway = FontFamily(Font(R.font.raleway_light))
val Alegreya = FontFamily(Font(R.font.alegreya_black_italic))

val AppTypography = appTypography(
    regular = Regular,
    bold = Bold,
    light = Light,
    italic = Italic,
)

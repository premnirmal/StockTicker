package com.github.premnirmal.ticker.widget

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.LocalContext
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders

object WidgetColors {
    @Composable
    fun colors(): ColorProviders {
        val context = LocalContext.current
        val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        return if (dynamicColor) {
            ColorProviders(
                light = dynamicLightColorScheme(context),
                dark = dynamicDarkColorScheme(context),
            )
        } else {
            ColorProviders(
                scheme = MaterialTheme.colorScheme
            )
        }
    }
}

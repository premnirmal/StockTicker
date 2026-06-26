package com.github.premnirmal.tickerwidget.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The shared cross-platform Material 3 [Shapes] used by both the Android and iOS app themes, so the
 * corner radii are defined once in `commonMain`.
 */
val appShapes = Shapes(
  small = RoundedCornerShape(24.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(8.dp)
)

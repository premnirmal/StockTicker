package com.github.premnirmal.ticker.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Created by premnirmal on 2/28/16.
 */
class AnalyticsImpl(
  @ApplicationContext context: Context,
  generalProperties: dagger.Lazy<GeneralProperties>
) : Analytics
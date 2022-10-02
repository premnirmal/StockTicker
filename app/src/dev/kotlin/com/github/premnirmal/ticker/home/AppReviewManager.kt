package com.github.premnirmal.ticker.home

import android.content.Context
import com.github.premnirmal.ticker.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext

class AppReviewManager(@ApplicationContext context: Context, appPreferences: AppPreferences) : IAppReviewManager
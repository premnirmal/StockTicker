package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.tickerwidget.BuildConfig

class CommitsProvider {

    fun loadWhatsNew(): FetchResult<List<String>> {
        val changeLog = BuildConfig.CHANGE_LOG.split("\n")
            .filterNot {
                // To avoid noise, remove version code bump commits that were done by github actions bot
                it.contains("Updated version.properties") || it.contains("F-droid")
            }
        return FetchResult.success(changeLog)
    }
}

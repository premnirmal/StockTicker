package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.model.FetchResult

/**
 * Exposes the "what's new" changelog (the git history baked into the build) as a list of lines.
 *
 * Part of the Phase 2 networking/provider migration: like [StocksApi]/`NewsProvider` it is a plain,
 * platform-agnostic class with no Android dependencies. The raw [changeLog] text is supplied by the
 * platform (`:app` passes `BuildConfig.CHANGE_LOG` via `NetworkModule.provideCommitsProvider`; the
 * iOS app will supply its own once it exists).
 */
class CommitsProvider(private val changeLog: String) {

    fun loadWhatsNew(): FetchResult<List<String>> {
        val lines = changeLog.split("\n")
            .filterNot {
                // To avoid noise, remove version code bump commits that were done by github actions bot
                it.contains("Updated version.properties") || it.contains("F-droid")
            }
        return FetchResult.success(lines)
    }
}

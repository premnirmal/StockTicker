package com.github.premnirmal.ticker.network

import com.github.premnirmal.shared.ChangelogBuildConfig
import com.github.premnirmal.ticker.model.FetchResult

/**
 * Exposes the "what's new" changelog (the local git history baked into the build) as a list of lines.
 *
 * Part of the Phase 2 networking/provider migration: like [StocksApi]/`NewsProvider` it is a plain,
 * platform-agnostic class with no Android dependencies. The changelog is generated from git into the
 * shared [ChangelogBuildConfig.CHANGE_LOG] constant (see the `:shared` `generateChangelog` task), so
 * both Android and iOS share the same source — no platform input is required.
 */
class CommitsProvider(private val changeLog: String = ChangelogBuildConfig.CHANGE_LOG) {

    fun loadWhatsNew(): FetchResult<List<String>> {
        val lines = changeLog.split("\n")
            .filterNot {
                // To avoid noise, remove version code bump commits that were done by github actions bot
                it.contains("Updated version.properties") || it.contains("F-droid")
            }
        return FetchResult.success(lines)
    }
}

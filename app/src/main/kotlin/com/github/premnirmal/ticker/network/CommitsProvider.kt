package com.github.premnirmal.ticker.network

import android.annotation.SuppressLint
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.RepoCommit
import com.github.premnirmal.tickerwidget.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommitsProvider @Inject constructor(private val githubApi: GithubApi) {

  private var cachedChanges: List<RepoCommit>? = null

  fun initCache() {
    GlobalScope.launch { fetchRepoCommits() }
  }

  private suspend fun fetchRepoCommits(): FetchResult<List<RepoCommit>> {
    cachedChanges?.let { return FetchResult.success(it) }
    return withContext(Dispatchers.IO) {
      try {
        val currentVersion = BuildConfig.VERSION_CODE
        val comparison = githubApi.compareTags(
            "${currentVersion - 6}",
            "$currentVersion"
        )
        val commits = comparison.commits.asReversed()
        cachedChanges = commits
        return@withContext FetchResult.success(commits)
      } catch (ex: Exception) {
        Timber.w(ex)
        return@withContext FetchResult.failure(ex)
      }
    }
  }

  @SuppressLint("DefaultLocale")
  suspend fun fetchWhatsNew(): FetchResult<List<String>> {
    with(fetchRepoCommits()) {
      return if (wasSuccessful) {
        FetchResult.success(data.map {
          it.commit.message.replace("\n", "").capitalize()
        })
      } else FetchResult.failure(error)
    }
  }
}

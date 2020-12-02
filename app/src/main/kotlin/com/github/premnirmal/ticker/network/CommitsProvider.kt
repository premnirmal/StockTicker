package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.FetchResult.Companion
import com.github.premnirmal.ticker.network.data.RepoCommit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommitsProvider @Inject constructor(private val githubApi: GithubApi) {

  private var cachedCommits: List<RepoCommit>? = null

  fun initCache() {
    GlobalScope.launch { fetchRepoCommits() }
  }

  private suspend fun fetchRepoCommits(): FetchResult<List<RepoCommit>> = withContext(Dispatchers.IO) {
    try {
      cachedCommits?.let { return@withContext FetchResult.success(it) }
      val commits = githubApi.getRepoCommits()
      cachedCommits = commits
      return@withContext Companion.success(commits)
    } catch (ex: Exception) {
      Timber.w(ex)
      return@withContext FetchResult.failure<List<RepoCommit>>(ex)
    }
  }

  suspend fun fetchWhatsNew(): List<String>? {
    with(fetchRepoCommits()) {
      return if (wasSuccessful) {
        data.take(10)
            .map { it.commit.message.replace("\n", "") }
      } else null
    }
  }
}

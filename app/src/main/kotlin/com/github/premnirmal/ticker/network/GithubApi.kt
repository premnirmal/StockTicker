package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.RepoCommit
import retrofit2.http.GET

interface GithubApi {

  @GET("repos/premnirmal/stockticker/commits")
  suspend fun getRepoCommits(): List<RepoCommit>
}
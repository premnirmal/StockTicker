package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

data class TagComparison(
  @SerializedName("url") val url: String,
  @SerializedName("html_url") val html_url: String,
  @SerializedName("commits") val commits: List<RepoCommit>,
  @SerializedName("status") val status: String,
  @SerializedName("ahead_by") val aheadBy: Int,
  @SerializedName("behind_by") val behindBy: Int,
  @SerializedName("total_commits") val totalCommits: Int
)

data class RepoCommit(
  @SerializedName("sha") val sha: String,
  @SerializedName("node_id") val node_id: String,
  @SerializedName("url") val url: String,
  @SerializedName("html_url") val html_url: String,
  @SerializedName("commit") val commit: Commit,
  @SerializedName("author") val author: Author
)

data class Commit(
  @SerializedName("author") val author: Committer,
  @SerializedName("committer") val committer: Committer,
  @SerializedName("message") val message: String
)

data class Committer(
  @SerializedName("name") val name: String,
  @SerializedName("email") val email: String
)

data class Author(
  @SerializedName("login") val login: String,
  @SerializedName("id") val id: Long,
  @SerializedName("type") val type: String,
  @SerializedName("url") val url: String
)
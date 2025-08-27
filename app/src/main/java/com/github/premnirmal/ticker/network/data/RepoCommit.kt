package com.github.premnirmal.ticker.network.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagComparison(
    @SerialName("url") val url: String,
    @SerialName("html_url") val html_url: String,
    @SerialName("commits") val commits: List<RepoCommit>,
    @SerialName("status") val status: String,
    @SerialName("ahead_by") val aheadBy: Int,
    @SerialName("behind_by") val behindBy: Int,
    @SerialName("total_commits") val totalCommits: Int
)

@Serializable
data class RepoCommit(
    @SerialName("sha") val sha: String,
    @SerialName("node_id") val node_id: String,
    @SerialName("url") val url: String,
    @SerialName("html_url") val html_url: String,
    @SerialName("commit") val commit: Commit,
    @SerialName("author") val author: Author
)

@Serializable
data class Commit(
    @SerialName("author") val author: Committer,
    @SerialName("committer") val committer: Committer,
    @SerialName("message") val message: String
)

@Serializable
data class Committer(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String
)

@Serializable
data class Author(
    @SerialName("login") val login: String,
    @SerialName("id") val id: Long,
    @SerialName("type") val type: String,
    @SerialName("url") val url: String
)

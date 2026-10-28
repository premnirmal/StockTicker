package com.github.premnirmal.gradle

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project

fun Project.execAndGetStdout(vararg args: String): String =
  providers.exec {
    commandLine(*args)
  }.standardOutput.asText.get().trim()

fun Project.getVersionNameFromGit(): String =
  execAndGetStdout("git", "describe", "--tags", "--abbrev=0")

fun Project.getOldGitVersionFromGit(): String =
  try {
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
      execAndGetStdout("powershell", "-command", "git tag --sort=-committerdate | Select-Object -first 15 | Select-Object -last 1")
    } else {
      execAndGetStdout("sh", "-c", "git tag --sort=-committerdate | head -20 | tail -1")
    }
  } catch (e: Exception) {
    println(e.message)
    println(e.stackTrace)
    "1.0"
  }

fun Project.getCommitsBetween(old: String, new: String): String =
  try {
    val log =
      if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        execAndGetStdout("powershell", "-command", "git log --pretty=format:\"%s\" $old...$new")
      } else {
        execAndGetStdout("sh", "-c", "git log --pretty=format:\"%s\" $old...$new")
      }
    log.replace("\n", "\\n")
  } catch (e: Exception) {
    println(e.message)
    println(e.stackTrace)
    throw e
  }


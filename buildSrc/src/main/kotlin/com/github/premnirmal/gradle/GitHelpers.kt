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
    val count = if (Os.isFamily(Os.FAMILY_WINDOWS)) 15 else 20
    execAndGetStdout("git", "tag", "--sort=-committerdate")
      .lineSequence().filter(String::isNotBlank).take(count).lastOrNull() ?: getVersionNameFromGit()
  } catch (e: Exception) {
    println(e.message)
    println(e.stackTrace)
    "1.0"
  }

fun Project.getCommitsBetween(old: String, new: String): String =
  try {
    val log = execAndGetStdout("git", "log", "--pretty=format:%s", "$old...$new")
    log.replace("\n", "\\n")
  } catch (e: Exception) {
    println(e.message)
    println(e.stackTrace)
    throw e
  }


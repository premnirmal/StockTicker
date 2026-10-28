package com.github.premnirmal.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

abstract class UpdateVersionPropertiesTask : DefaultTask() {

  @TaskAction
  fun updateVersionPropertiesFile() {
    logger.lifecycle("Updating version.properties file for F-droid")

    val versionName = project.getVersionNameFromGit()
    val (major, minor, patch) = parseSemanticVersion(versionName)
    val versionCode = (major * 100000000) + (minor * 100000) + patch

    val output = buildString {
      append("versionName=$versionName")
      append("\n")
      append("versionCode=$versionCode")
    }

    val versionPropertiesFile = project.rootProject.file("app/version.properties")
    val previousOutput =
      if (versionPropertiesFile.exists()) {
        versionPropertiesFile.readText()
      } else {
        ""
      }

    if (previousOutput != output) {
      versionPropertiesFile.writeText(output)
      logger.lifecycle("Updated app/version.properties to versionName=$versionName, versionCode=$versionCode")
    } else {
      logger.lifecycle("app/version.properties has not changed (versionName=$versionName, versionCode=$versionCode)")
    }
  }

  private fun parseSemanticVersion(versionName: String): Triple<Int, Int, Int> {
    val parts = versionName.split(".")
    if (parts.size < 3) {
      throw GradleException("Expected version tag in the format x.y.z but got '$versionName'")
    }

    val major = parts[0].toIntOrNull()
    val minor = parts[1].toIntOrNull()
    val patch = parts[2].toIntOrNull()
    if (major == null || minor == null || patch == null) {
      throw GradleException("Expected numeric version parts in tag '$versionName'")
    }

    return Triple(major, minor, patch)
  }
}

class VersionPropertiesTaskPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    if (target != target.rootProject) return

    target.tasks.register("updateVersionPropertiesFile", UpdateVersionPropertiesTask::class.java) {
      group = "versioning"
      description = "Updates app/version.properties from the latest git tag."
    }
  }
}


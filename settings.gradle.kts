pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  // See https://splitties.github.io/refreshVersions
  id("de.fayard.refreshVersions") version "0.60.6"
}
include(":app")
include(":UI")

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  extra["kotlin_version"] = "2.0.0"
  repositories {
    mavenCentral()
    google()
  }
  dependencies {
    classpath(Android.tools.build.gradlePlugin)
    classpath(Google.playServicesGradlePlugin)
    classpath(Firebase.crashlyticsGradlePlugin)
    classpath(kotlin("gradle-plugin", version = "2.0.0"))
    classpath(Google.dagger.hilt.android.gradlePlugin)
  }
}
plugins {
  // Existing plugins
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.dagger.hilt) apply false
}
project.extra["preDexLibs"] = !project.hasProperty("disablePreDex")
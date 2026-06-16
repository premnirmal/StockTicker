import com.github.premnirmal.gradle.VersionPropertiesTaskPlugin

// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.hilt) apply false
  alias(libs.plugins.com.google.devtools.ksp) apply false
  alias(libs.plugins.androidx.room) apply false
  id("com.google.gms.google-services") version "4.4.4" apply false
  id("com.google.firebase.crashlytics") version "3.0.7" apply false
}

apply<VersionPropertiesTaskPlugin>()

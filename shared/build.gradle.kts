import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.github.premnirmal.gradle.getCommitsBetween
import com.github.premnirmal.gradle.getOldGitVersionFromGit
import com.github.premnirmal.gradle.getVersionNameFromGit

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.com.google.devtools.ksp)
  alias(libs.plugins.androidx.room)
  id("kotlin-parcelize")
}

room {
  schemaDirectory("$projectDir/schemas")
}

repositories {
  google()
  mavenCentral()
  maven("https://jitpack.io")
}

// Generates the "what's new" changelog from the local git history into a commonMain constant, so the
// changelog is shared by every platform (Android and iOS) instead of living only in :app's Android
// BuildConfig. CommitsProvider defaults to this constant.
val generatedChangelogDir: Provider<Directory> =
    layout.buildDirectory.dir("generated/changelog/commonMain/kotlin")

val generateChangelog by tasks.registering {
  val outputDir = generatedChangelogDir
  outputs.dir(outputDir)
  val versionName = project.getVersionNameFromGit()
  val oldVersion = project.getOldGitVersionFromGit()
  // git log subjects already have their newlines escaped to the literal "\n" sequence.
  val changeLog = project.getCommitsBetween(old = oldVersion, new = versionName)
  inputs.property("changeLog", changeLog)
  doLast {
    // Escape the characters that would break a Kotlin string literal. Backslashes are intentionally
    // preserved so the embedded "\n" line separators survive as real newlines at runtime.
    val escaped = changeLog
        .replace("\"", "\\\"")
        .replace("\$", "\\\$")
    val pkgDir = outputDir.get().asFile.resolve("com/github/premnirmal/shared")
    pkgDir.mkdirs()
    pkgDir.resolve("ChangelogBuildConfig.kt").writeText(
        """
        |package com.github.premnirmal.shared
        |
        |/**
        | * Generated at build time from the local git history (see :shared `generateChangelog`).
        | * Shared by every platform so Android and iOS show the same "what's new" changelog.
        | */
        |internal object ChangelogBuildConfig {
        |    const val CHANGE_LOG: String = "$escaped"
        |}
        |
        """.trimMargin()
    )
  }
}

kotlin {
  androidTarget {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
    // kotlin-parcelize only recognises `kotlinx.parcelize.Parcelize` by default. Our shared
    // models are annotated with the multiplatform `@CommonParcelize` alias, so we register it
    // as an additional Parcelize annotation on the Android compilations (where the plugin runs)
    // to ensure the real Parcelable implementations are generated.
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions {
          freeCompilerArgs.addAll(
              "-P",
              "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.github.premnirmal.shared.CommonParcelize"
          )
        }
      }
    }
  }

  listOf(
      iosArm64(),
      iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "Shared"
      isStatic = true
    }
  }

  sourceSets {
    commonMain {
      kotlin.srcDir(generateChangelog)
    }
    commonMain.dependencies {
      implementation(KotlinX.serialization.json)
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:_")
      // Phase 3: shared ViewModels live in commonMain. AndroidX Lifecycle's ViewModel/viewModelScope
      // are multiplatform (2.8+), so the same ViewModel runs on Android and iOS.
      api("androidx.lifecycle:lifecycle-viewmodel:_")
      implementation("io.ktor:ktor-client-core:_")
      implementation("io.ktor:ktor-client-content-negotiation:_")
      implementation("io.ktor:ktor-serialization-kotlinx-json:_")
      implementation("io.github.pdvrieze.xmlutil:serialization:0.91.2")
      api(libs.room.runtime)
      implementation(libs.androidx.sqlite.bundled)
      implementation(libs.koin.core)
      implementation("androidx.datastore:datastore-preferences-core:_")
      implementation("com.squareup.okio:okio:3.9.1")
    }
    androidMain.dependencies {
      implementation("io.ktor:ktor-client-okhttp:_")
      implementation(JakeWharton.timber)
    }
    iosMain.dependencies {
      implementation("io.ktor:ktor-client-darwin:_")
      implementation(libs.kotlinx.datetime)
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation("io.ktor:ktor-client-mock:_")
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:_")
    }
  }
}

android {
  namespace = "com.github.premnirmal.shared"
  compileSdk = 36

  defaultConfig {
    minSdk = 26
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

// Room KSP processor for every Kotlin target that contains the shared persistence engine
// (the @Database/@Dao/@Entity declarations live in commonMain).
dependencies {
  add("kspAndroid", libs.room.compiler)
  add("kspIosArm64", libs.room.compiler)
  add("kspIosSimulatorArm64", libs.room.compiler)
}

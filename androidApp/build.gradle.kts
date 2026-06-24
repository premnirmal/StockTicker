import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.github.premnirmal.gradle.getOldGitVersionFromGit
import com.github.premnirmal.gradle.getVersionNameFromGit
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("kotlin-parcelize")
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
  alias(libs.plugins.com.google.devtools.ksp)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.detekt.plugin)
  alias(libs.plugins.kotlinx.serialization)
}

detekt {
  toolVersion = libs.versions.detekt.get()
  config.setFrom(files("../config/detekt/detekt.yml", "../config/detekt/detekt-formatting.yml"))
  buildUponDefaultConfig = true
  autoCorrect = true
}

buildscript {
  repositories {
    mavenCentral()
    google()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
  }
  dependencies {
    classpath(kotlin("gradle-plugin", version = "2.0.0"))
  }
}

repositories {
  mavenCentral()
  maven("https://oss.sonatype.org/content/repositories/snapshots/")
  maven("https://jitpack.io")
  maven("https://maven.google.com")
}

android {
  buildFeatures {
    buildConfig = true
  }

  namespace = "com.github.premnirmal.tickerwidget"
  compileSdk = 36
  buildToolsVersion = "31.0.0"

  val name = project.getVersionNameFromGit()
  val major = name.split(".")[0].toInt()
  val minor = name.split(".")[1].toInt()
  val patch = name.split(".")[2].toInt()
  val code = (major * 100000000) + (minor * 100000) + patch
  val oldGitVersion = project.getOldGitVersionFromGit()
  println("get version name $name")
  println("Old git version $oldGitVersion")
  val appIdBase = "com.github.premnirmal.tickerwidget"

  defaultConfig {
    applicationId = appIdBase
    minSdk = 26
    targetSdk = 36

    versionCode = code
    versionName = name

    buildConfigField("String", "PREVIOUS_VERSION", "\"$oldGitVersion\"")
  }

  signingConfigs {
    create("release") {
      storeFile = file("keystore.jks")

      val propsFile: File = file("keystore.properties")
      if (propsFile.exists()) {
        val props: Properties = Properties()
        props.load(FileInputStream(propsFile))
        storePassword = props.getProperty("key.store.password")
        keyPassword = props.getProperty("key.alias.password")
        keyAlias = props.getProperty("key.alias.alias")
      }
    }
  }

  flavorDimensions += "mobile"

  productFlavors {
    create("dev") {
      dimension = "mobile"
      applicationId = "$appIdBase.dev"
    }
    create("prod") {
      dimension = "mobile"
      applicationId = appIdBase
    }
    create("purefoss") {
      dimension = "mobile"
      applicationId = appIdBase
    }
  }

  bundle {
    density {
      enableSplit = true
    }
    abi {
      enableSplit = true
    }
    language {
      enableSplit = false
    }
  }

  buildTypes {
    release {
      isDebuggable = false
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = true
      setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
    }
    debug {
      isDebuggable = true
      extra["enableCrashlytics"] = false
      isMinifyEnabled = false
      configure<CrashlyticsExtension> {
        mappingFileUploadEnabled = false
      }
      setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
    }
  }

  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.8"
  }

  packaging {
    resources {
      excludes +=
          listOf("META-INF/DEPENDENCIES", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE.txt")
    }
  }

  testOptions {
    unitTests {
      isReturnDefaultValues = true
      isIncludeAndroidResources  = true
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

// Guard the Jetpack Navigation against the 2.10.x pre-release line: the CMP navigation-compose
// in :shared is pinned to the stable 2.9.2 line, but force back to 2.8.5 if anything transitively
// requests 2.10.x android artifacts (which require compileSdk 37).
configurations.configureEach {
  resolutionStrategy.eachDependency {
    if (requested.group == "androidx.navigation" && requested.version?.startsWith("2.10") == true) {
      useVersion("2.8.5")
    }
  }
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.androidx.core.ktx)

  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.browser)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.navigation.compose.jetpack)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.ui.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material3.android)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  implementation(libs.androidx.glance.appwidget.preview)
  implementation(libs.androidx.glance.preview)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.compose.material3.window.size)
  implementation(libs.accompanist.adaptive)
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.compose.runtime.livedata)
  implementation(libs.androidx.datastore.preferences)

  implementation(project(":shared"))

  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  implementation(libs.javax.inject)
  implementation(libs.javax.annotation.api)

  implementation(libs.koin.android)
  implementation(libs.koin.androidx.compose)
  implementation(libs.koin.androidx.workmanager)

  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.kotlinx.serialization.json)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.lifecycle.common.java8)
  implementation(libs.androidx.work.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.datastore)
  implementation(libs.androidx.datastore.preferences)

  implementation(libs.timber)

  detektPlugins(libs.detekt.formatting)
  detektPlugins(libs.detekt.compose)

  "prodImplementation"(libs.google.play.review)
  "prodImplementation"(libs.google.play.review.ktx)

  "prodImplementation"(platform("com.google.firebase:firebase-bom:34.1.0"))
  "prodImplementation"("com.google.firebase:firebase-analytics")
  "prodImplementation"("com.google.firebase:firebase-crashlytics-ndk")

  //  debugImplementation(Square.leakCanary.android)

  testImplementation(libs.koin.test)
  testImplementation(libs.koin.test.junit4)

  testImplementation(libs.junit)
  testImplementation(libs.assertj.core)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.runner)
  testImplementation(libs.androidx.test.rules)
  testImplementation(libs.androidx.annotation)
  testImplementation(libs.androidx.test.rules)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.work.testing)

  // Need this to fix a class not found error in tests (https://github.com/robolectric/robolectric/issues/1932)
  testImplementation(libs.opengl.api)
}

// Remove google play services and crashlytics plugin for non prod builds
android {
  androidComponents {
    onVariants { variant ->
      println("Variant: ${variant.name}, buildType: ${variant.buildType}, flavor: ${variant.flavorName}")
      if (!variant.name.lowercase(Locale.getDefault()).contains("prod")) {
        val googleTask =
          tasks.findByName("process${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}GoogleServices")
        val crashlyticsMappingTask =
          tasks.findByName("uploadCrashlyticsMappingFile${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}")
        googleTask?.let {
          println("disabling ${googleTask.name}")
          googleTask.enabled = false
        }
        crashlyticsMappingTask?.let {
          println("disabling ${crashlyticsMappingTask.name}")
          crashlyticsMappingTask.enabled = false
        }
      }
    }
  }
}

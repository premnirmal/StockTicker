import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.apache.tools.ant.taskdefs.condition.Os
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("kotlin-parcelize")
  id("com.google.gms.google-services")
  id("com.google.firebase.crashlytics")
  alias(libs.plugins.com.google.devtools.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.detekt.plugin)
  alias(libs.plugins.kotlinx.serialization)
}

detekt {
  toolVersion = libs.versions.detekt.get()
  config.setFrom(file("../config/detekt/detekt.yml"))
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
  val getVersionName = { ->
    ByteArrayOutputStream().use { outputStream ->
      val stdout = outputStream
      exec {
        commandLine("git", "describe", "--tags", "--abbrev=0")
        standardOutput = stdout
      }
      stdout.toString().trim()
    }
  }

  val getOldGitVersion = { ->
    try {
      ByteArrayOutputStream().use { stdout ->
        exec {
          if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            commandLine("powershell", "-command", "git tag --sort=-committerdate | Select-Object -first 10 | Select-Object -last 1")
          } else {
            commandLine("sh", "-c", "git tag --sort=-committerdate | head -10 | tail -1")
          }
          standardOutput = stdout
        }
        stdout.toString().trim()
      }
    } catch (e: Exception) {
      println(e.message)
      println(e.stackTrace)
      "1.0"
    }
  }

    fun getCommitsBetween(old: String, new: String): String {
        try {
            ByteArrayOutputStream().use { stdout ->
                exec {
                    commandLine("sh", "-c", "git log --pretty=format:\"%s\" $old...$new")
                    standardOutput = stdout
                }
                return stdout.toString().trim().replace("\n", "\\n")
            }
        } catch (e: Exception) {
            println(e.message)
            println(e.stackTrace)
            throw e
        }
    }

  buildFeatures {
    buildConfig = true
  }

  namespace = "com.github.premnirmal.tickerwidget"
  compileSdk = 36
  buildToolsVersion = "31.0.0"

  val name = getVersionName()
  val major = name.split(".")[0].toInt()
  val minor = name.split(".")[1].toInt()
  val patch = name.split(".")[2].toInt()
  val code = (major * 100000000) + (minor * 100000) + patch
  val oldGitVersion = getOldGitVersion()
  val changeLog = getCommitsBetween(old = oldGitVersion, new = name)
  println("get version name $name")
  println("Old git version $oldGitVersion")
  println("Change log:\n $changeLog")
  val appIdBase = "com.github.premnirmal.tickerwidget"

  defaultConfig {
    applicationId = appIdBase
    minSdk = 26
    targetSdk = 36

    versionCode = code
    versionName = name

    buildConfigField("String", "PREVIOUS_VERSION", "\"$oldGitVersion\"")
    buildConfigField("String", "CHANGE_LOG", "\"$changeLog\"")
  }

  dexOptions {
    javaMaxHeapSize = "2048M"
  }

  signingConfigs {
    create("release") {
      storeFile = file("file:keystore.jks")

      val propsFile: File = file("file:keystore.properties")
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
      applicationId = appIdBase + ".dev"
    }
    create("prod") {
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

  packagingOptions {
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
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
  }
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(AndroidX.core.ktx)

  implementation(AndroidX.appCompat)
  implementation(AndroidX.browser)
  implementation(AndroidX.core.splashscreen)
  implementation(AndroidX.activity.compose)
  implementation(AndroidX.navigation.compose)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.ui.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material3.android)
  implementation(libs.androidx.compose.ui.tooling.preview)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(AndroidX.lifecycle.viewModelCompose)
  implementation(AndroidX.compose.material3.windowSizeClass)
  implementation(Google.accompanist.adaptive)
  implementation(libs.accompanist.permissions)
  implementation(AndroidX.compose.runtime.liveData)
  implementation(AndroidX.hilt.navigationCompose)
  implementation(AndroidX.dataStore.preferences)
  implementation(libs.reorderable)

  implementation(project(":UI"))

  implementation(COIL)
  implementation(COIL.compose)

  implementation(libs.javax.inject)
  implementation(libs.javax.annotation.api)

  implementation(AndroidX.compose.ui.toolingPreview)
  implementation(libs.hilt)
  implementation(libs.androidx.hilt)
  ksp(libs.hilt.android.compiler)

  implementation(Square.okHttp3)
  implementation(Square.okHttp3.loggingInterceptor)
  implementation(Square.retrofit2)
  implementation(Square.retrofit2.converter.simpleXml)
  implementation(Square.retrofit2.converter.scalars)
  implementation(libs.retrofit.kotlin.serialization)
  implementation(libs.jsoup)
  implementation(KotlinX.serialization.json)

  implementation(KotlinX.coroutines.android)
  implementation(AndroidX.lifecycle.runtime.ktx)
  implementation(AndroidX.lifecycle.viewModelKtx)
  implementation(AndroidX.lifecycle.liveDataKtx)
  implementation(AndroidX.lifecycle.commonJava8)
  implementation(AndroidX.work.runtime)
  implementation(AndroidX.work.runtimeKtx)
  implementation(AndroidX.dataStore)

  implementation(JakeWharton.timber)
  implementation(libs.mpandroidchart)

  implementation(libs.room.runtime)
  implementation(libs.room.ktx)

  detektPlugins(libs.detekt.formatting)
  detektPlugins(libs.detekt.compose)

  ksp(libs.room.compiler)

  "prodImplementation"(Google.android.play.review)
  "prodImplementation"(Google.android.play.reviewKtx)

  "prodImplementation"(platform("com.google.firebase:firebase-bom:34.1.0"))
  "prodImplementation"("com.google.firebase:firebase-analytics")
  "prodImplementation"("com.google.firebase:firebase-crashlytics-ndk")

  //  debugImplementation(Square.leakCanary.android)

  testImplementation(Google.dagger.hilt.android.testing)
  kspTest(libs.hilt.android.compiler)

  testImplementation(Testing.junit4)
  testImplementation(Testing.assertj.core)
  testImplementation(Testing.robolectric)
  testImplementation(AndroidX.test.runner)
  testImplementation(AndroidX.test.rules)
  testImplementation(AndroidX.annotation)
  testImplementation(AndroidX.test.rules)
  testImplementation(Testing.mockito.core)
  testImplementation(Testing.mockito.kotlin)
  testImplementation(KotlinX.coroutines.test)
  testImplementation(libs.room.testing)

  // Need this to fix a class not found error in tests (https://github.com/robolectric/robolectric/issues/1932)
  testImplementation(libs.opengl.api)
}

// Remove google play services and crashlytics plugin for non prod builds
android {
  androidComponents {
    onVariants {
      println("Variant: ${it.name}, buildType: ${it.buildType}, flavor: ${it.flavorName}")
      if (!it.name.lowercase(Locale.getDefault()).contains("prod")) {
        val googleTask =
          tasks.findByName("process${it.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}GoogleServices")
        val crashlyticsMappingTask =
          tasks.findByName("uploadCrashlyticsMappingFile${it.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}")
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

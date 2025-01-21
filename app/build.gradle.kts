import org.apache.tools.ant.taskdefs.condition.Os
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("kotlin-parcelize")
  id("com.google.gms.google-services")
  id("com.google.firebase.crashlytics")
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.dagger.hilt)
  alias(libs.plugins.com.google.devtools.ksp)
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

  buildFeatures {
    buildConfig = true
  }

  namespace = "com.github.premnirmal.tickerwidget"
  compileSdk = 35
  buildToolsVersion = "31.0.0"

  val name = getVersionName()
  val major = name.split(".")[0].toInt()
  val minor = name.split(".")[1].toInt()
  val patch = name.split(".")[2].toInt()
  val code = (major * 100000000) + (minor * 100000) + patch
  val oldGitVersion = getOldGitVersion()
  println("get version name $name")
  println("Old git version $oldGitVersion")
  val appIdBase = "com.github.premnirmal.tickerwidget"

  defaultConfig {
    applicationId = appIdBase
    minSdk = 26
    targetSdk = 34

    versionCode = code
    versionName = name

    buildConfigField("String", "PREVIOUS_VERSION", "\"$oldGitVersion\"")

    // room {
    //   schemaDirectory("$projectDir/schemas")
    // }
  }
  // resources.sourceSets["test"].resources {
  //   srcDirs("src/test/resources")
  // }

  dexOptions {
    javaMaxHeapSize = "2048M"
  }

  buildFeatures {
    viewBinding = true
  }

  signingConfigs {
    create("release") {
      storeFile = file("file:keystore.jks:_")

      val propsFile: File = file("file:keystore.properties:_")
      if (propsFile.exists()) {
        val props: Properties = Properties()
        props.load(FileInputStream(propsFile))
        storePassword = props.getProperty("key.store.password")
        keyPassword = props.getProperty("key.alias.password")
        keyAlias = props.getProperty("key.alias.alias")
      }
    }
  }

  flavorDimensions("mobile")

  productFlavors {
    create("dev") {
      dimension = "mobile"
      applicationId = appIdBase + ".dev"
    }
    create("prod") {
      dimension = "mobile"
      applicationId = appIdBase
    }
    create("purefoss") {
      dimension = "mobile"
      // no analytics, but still using the production packageName
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
      setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))
    }
    debug {
      isDebuggable = true
      extra["enableCrashlytics"] = false
      isMinifyEnabled = true
      firebaseCrashlytics {
        mappingFileUploadEnabled = false
      }
      setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))
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
  implementation(libs.legacy.support.v4)
  implementation(Google.android.material)
  implementation(AndroidX.preference.ktx)
  implementation(AndroidX.browser)

  implementation(AndroidX.constraintLayout)
  implementation(AndroidX.viewPager2)
  implementation(AndroidX.core.splashscreen)
  implementation(AndroidX.fragment.ktx)
  implementation(AndroidX.activity.compose)
  implementation(AndroidX.navigation.compose)
  implementation(libs.material3.android)
  implementation(AndroidX.lifecycle.viewModelCompose)
  implementation(AndroidX.compose.material3.windowSizeClass)
  implementation(Google.accompanist.adaptive)
  implementation(libs.accompanist.pager)
  implementation(AndroidX.compose.runtime.liveData)
  implementation(AndroidX.hilt.navigationCompose)
  implementation(AndroidX.dataStore.preferences)

  implementation(project(":UI"))

  implementation(libs.reorderable)

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
  implementation(Square.retrofit2.converter.gson)
  implementation(Square.retrofit2.converter.simpleXml)
  implementation(Square.retrofit2.converter.scalars)
  implementation(libs.jsoup)

  implementation(KotlinX.coroutines.android)
  implementation(AndroidX.lifecycle.runtime.ktx)
  implementation(AndroidX.lifecycle.viewModelKtx)
  implementation(AndroidX.lifecycle.liveDataKtx)
  implementation(AndroidX.lifecycle.commonJava8)
  implementation(AndroidX.work.runtime)
  implementation(AndroidX.work.runtimeKtx)

  implementation(libs.threetenabp)

  implementation(JakeWharton.timber)
  implementation(libs.mpandroidchart)

  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)

  "prodImplementation"(Google.android.play.review)
  "prodImplementation"(Google.android.play.reviewKtx)

  implementation(libs.ticker)

  "prodImplementation"(Firebase.crashlytics)
  "prodImplementation"(Firebase.analytics)

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
  testImplementation(libs.threetenbp)
  testImplementation(Testing.mockito.core)
  testImplementation(Testing.mockito.kotlin)
  testImplementation(KotlinX.coroutines.test)
  testImplementation(libs.room.testing)

  // Need this to fix a class not found error in tests (https://github.com/robolectric/robolectric/issues/1932)
  testImplementation(libs.opengl.api)
}

// configurations.configureEach {
//   resolutionStrategy.force "org.objenesis:objenesis:_"
//   exclude(group = "xpp3",) module = "xpp3"
// }
//
// android.applicationVariants.configureEach { variant ->
//   if (!variant.name.toLowerCase().contains("prod")) {
//     val googleTask = tasks.findByName("process${variant.name.capi(talize()}GoogleServices"))
//     val crashlyticsMappingTask = tasks.
//         findByName("uploadCrashlyticsMappingFile${variant.name.capi(talize()}"))
//     println("flavour: ${variant.name}")
//     println("disabling ${googleTask.name}")
//     googleTask.enabled = false
//     if (crashlyticsMappingTask != null) {
//       println("disabling ${crashlyticsMappingTask.name}")
//       crashlyticsMappingTask.enabled = false
//     }
//   }
// }


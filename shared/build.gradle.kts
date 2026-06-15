import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlinx.serialization)
  id("kotlin-parcelize")
}

repositories {
  google()
  mavenCentral()
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
      iosX64(),
      iosArm64(),
      iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "Shared"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(KotlinX.serialization.json)
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:_")
      implementation("io.ktor:ktor-client-core:_")
      implementation("io.ktor:ktor-client-content-negotiation:_")
      implementation("io.ktor:ktor-serialization-kotlinx-json:_")
      implementation("io.github.pdvrieze.xmlutil:serialization:0.91.2")
    }
    androidMain.dependencies {
      implementation("io.ktor:ktor-client-okhttp:_")
      implementation(JakeWharton.timber)
    }
    iosMain.dependencies {
      implementation("io.ktor:ktor-client-darwin:_")
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

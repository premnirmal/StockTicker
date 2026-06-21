import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

repositories {
  google()
  mavenCentral()
  maven("https://jitpack.io")
}

kotlin {
  androidTarget {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }

  listOf(
      iosArm64(),
      iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "SharedUI"
      isStatic = true
      // Re-export the :shared API so the iOS app can link a single framework that exposes both the
      // shared core (Phase 0-3) and the shared Compose Multiplatform UI (Phase 4).
      export(project(":shared"))
    }
  }

  sourceSets {
    commonMain.dependencies {
      // The shared core (DTOs, networking, Room, Koin sharedModule, ViewModels). Exposed as `api`
      // so it can be re-exported through the SharedUI iOS framework.
      api(project(":shared"))

      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:_")

      // Compose Multiplatform: the shared in-app UI renders from these on Android and iOS.
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)

      // The multiplatform `viewModel()`/`collectAsStateWithLifecycle()` composables and Koin's
      // multiplatform `koinViewModel()` used by the shared screens (JetBrains' Compose Multiplatform
      // lifecycle artifacts; the Google `androidx.lifecycle` `-compose` artifacts are Android-only).
      api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
      api("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
      api(libs.koin.compose.viewmodel)
      implementation(libs.koin.core)

      // Compose Multiplatform navigation (JetBrains' multiplatform port of
      // androidx.navigation:navigation-compose; same `androidx.navigation` package). Exposed as
      // `api` so :app resolves the shared `HomeNavHost`/`HomeNavigationActions` and its own
      // NavHosts (RootGraph/HomeActivity) from a single multiplatform artifact instead of the
      // Android-only Google one.
      api("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")

      // Coil 3 multiplatform image loading (NewsCard). Exposed as `api` so :app can configure the
      // SingletonImageLoader's network fetcher.
      api(libs.coil.compose)
      api(libs.coil.network.ktor3)

      // Vico multiplatform charting for the price chart (PriceChartView).
      api(libs.vico.multiplatform)

      // Multiplatform drag-to-reorder for the watchlist staggered grid (WatchlistContent).
      implementation(libs.reorderable)
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
  }
}

android {
  namespace = "com.github.premnirmal.tickerwidget.uishared"
  compileSdk = 36

  defaultConfig {
    minSdk = 26
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

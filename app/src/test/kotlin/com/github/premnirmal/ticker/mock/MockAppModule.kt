package com.github.premnirmal.ticker.mock

import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.RxBus
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Module(includes = arrayOf(MockNetworkModule::class))
class MockAppModule(private val app: Context) {

  @Provides
  internal fun provideApplicationContext(): Context {
    return app
  }

  @Provides @Singleton internal fun provideClock(): AppClock {
    return Mocker.provide(AppClock::class.java)
  }

  @Provides
  @Singleton
  internal fun provideEventBus(): RxBus {
    return RxBus()
  }

  @Provides
  @Singleton
  internal fun provideDefaultSharedPreferences(context: Context): SharedPreferences {
    val sharedPreferences = context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPreferences
  }
}
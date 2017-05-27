package com.github.premnirmal.ticker.components

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.components.AppClock.AppClockImpl
import com.github.premnirmal.ticker.network.NetworkModule
import dagger.Module
import dagger.Provides

/**
 * Created by premnirmal on 3/3/16.
 */
@Module(includes = arrayOf(NetworkModule::class))
class AppModule(private val app: Context) {

  @Provides internal fun provideApplicationContext(): Context {
    return app
  }

  @Provides @javax.inject.Singleton internal fun provideClock(): AppClock {
    return AppClockImpl()
  }

  @Provides @javax.inject.Singleton internal fun provideEventBus(): RxBus {
    return RxBus()
  }

  @Provides @javax.inject.Singleton internal fun provideSharedPreferences(context: Context): SharedPreferences {
    val sharedPreferences = context.getSharedPreferences(Tools.PREFS_NAME, MODE_PRIVATE)
    return sharedPreferences
  }
}
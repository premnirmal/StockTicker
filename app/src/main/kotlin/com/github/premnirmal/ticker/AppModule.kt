package com.github.premnirmal.ticker

import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.AppClock.AppClockImpl
import com.github.premnirmal.ticker.network.NetworkModule
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Module(includes = arrayOf(NetworkModule::class))
class AppModule(private val app: Context) {

  @Provides internal fun provideApplicationContext(): Context {
    return app
  }

  @Provides @Singleton internal fun provideClock(): AppClock {
    return AppClockImpl()
  }

  @Provides @Singleton internal fun provideEventBus(): RxBus {
    return RxBus()
  }

  @Provides @Singleton internal fun provideSharedPreferences(context: Context): SharedPreferences {
    val sharedPreferences = context.getSharedPreferences(Tools.PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPreferences
  }
}
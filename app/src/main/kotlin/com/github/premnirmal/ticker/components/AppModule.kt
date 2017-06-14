package com.github.premnirmal.ticker.components

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock.AppClockImpl
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

  @Provides @Singleton internal fun provideDefaultSharedPreferences(
      context: Context): SharedPreferences {
    val sharedPreferences = context.getSharedPreferences(AppPreferences.PREFS_NAME, MODE_PRIVATE)
    return sharedPreferences
  }

  @Provides internal fun provideAppWidgetManager(): AppWidgetManager {
    return AppWidgetManager.getInstance(app)
  }
}
package com.github.premnirmal.ticker.mock

import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.RxBus
import dagger.Module
import dagger.Provides
import org.mockito.Mockito.mock
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Module(includes = arrayOf(MockApiModule::class))
class MockAppModule(private val app: Context) {

  @Provides
  internal fun provideApplicationContext(): Context {
    return app
  }

  @Provides
  @Singleton
  internal fun provideEventBus(): RxBus {
    return RxBus()
  }

  @Provides
  @Singleton
  internal fun provideSharedPreferences(context: Context): SharedPreferences {
    return mock(SharedPreferences::class.java)
  }
}
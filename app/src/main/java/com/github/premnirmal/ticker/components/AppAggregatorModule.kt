package com.github.premnirmal.ticker.components

import com.github.premnirmal.ticker.network.NetworkModule
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Created by premnirmal on 3/3/16.
 */

@InstallIn(SingletonComponent::class)
@Module(
    includes = [
      AppModule::class,
      NetworkModule::class
    ]
)
interface AppAggregatorModule
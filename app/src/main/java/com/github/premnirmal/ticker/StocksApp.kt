package com.github.premnirmal.ticker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.github.premnirmal.ticker.components.LoggingTree
import com.github.premnirmal.ticker.components.appModule
import com.github.premnirmal.ticker.components.viewModelModule
import com.github.premnirmal.ticker.di.sharedModule
import com.github.premnirmal.ticker.network.networkModule
import com.github.premnirmal.ticker.notifications.NotificationsHandler
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import timber.log.Timber

/**
 * Created by premnirmal on 2/26/16.
 */
open class StocksApp : Application(), KoinComponent {

    val appPreferences: AppPreferences by inject()

    private val notificationsHandler: NotificationsHandler by inject()

    private val widgetDataProvider: WidgetDataProvider by inject()

    override fun onCreate() {
        initLogger()
        startKoin {
            androidContext(this@StocksApp)
            modules(sharedModule, networkModule, appModule, viewModelModule)
        }
        super.onCreate()
        initImageLoader()
        AppCompatDelegate.setDefaultNightMode(appPreferences.nightMode.toAppCompatNightMode())
        initNotificationHandler()
        runBlocking { widgetDataProvider.refreshWidgetDataList() }
    }

    // Coil 3 loads the shared NewsCard's remote images over the existing Ktor stack.
    protected open fun initImageLoader() {
        SingletonImageLoader.setSafe { context: PlatformContext ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }
    }

    protected open fun initNotificationHandler() {
        notificationsHandler.initialize()
    }

    protected open fun initLogger() {
        Timber.plant(LoggingTree())
    }
}

package com.github.premnirmal.ticker

/**
 * Created by premnirmal on 2/26/16.
 */
class Injector private constructor(app: BaseApp) {

    private val component: AppComponent

    init {
        component = DaggerAppComponent.builder()
                .appModule(AppModule(app))
                .build()
        instance = this
    }

    companion object {

        private var instance: Injector? = null

        internal fun init(app: BaseApp) {
            instance = Injector(app)
        }

        fun getAppComponent(): AppComponent {
            return instance!!.component
        }
    }

}
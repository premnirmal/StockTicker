package com.github.premnirmal.ticker

import dagger.ObjectGraph

/**
 * Created by premnirmal on 2/26/16.
 */
class Injector private constructor(app: StocksApp) {

    private val objectGraph: ObjectGraph

    init {
        objectGraph = ObjectGraph.create(AppModule(app))
        instance = this
    }

    companion object {

        private var instance: Injector? = null

        internal fun init(app: StocksApp) {
            instance = Injector(app)
        }

        @JvmStatic operator fun <T> get(clazz: Class<T>): T {
            return instance!!.objectGraph.get(clazz)
        }

        @JvmStatic fun inject(`object`: Any) {
            instance!!.objectGraph.inject(`object`)
        }
    }

}
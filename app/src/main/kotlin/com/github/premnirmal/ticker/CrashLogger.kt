package com.github.premnirmal.ticker

abstract class CrashLogger {

    private object Holder { val INSTANCE = CrashLoggerImpl() }

    companion object {
        private val instance: CrashLogger by lazy { Holder.INSTANCE }
        fun logException(throwable: Throwable) {
            instance.log(throwable)
        }
    }

    abstract fun log(throwable: Throwable)
}
package com.github.premnirmal.ticker.repo

import com.github.premnirmal.ticker.settings.SettingsStore
import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserDefaultsTickersStoreTest {

    private val suiteName = "test-tickers-${kotlin.random.Random.nextInt()}"
    private val defaults = NSUserDefaults(suiteName = suiteName)
    private val tickersStore = UserDefaultsTickersStore(SettingsStore(defaults))

    @AfterTest
    fun tearDown() {
        defaults.removePersistentDomainForName(suiteName)
    }

    @Test
    fun emptyByDefault() {
        assertTrue(tickersStore.readTickers().isEmpty())
    }

    @Test
    fun roundTrip() {
        tickersStore.saveTickers(setOf("AAPL", "GOOG", "MSFT"))
        assertEquals(setOf("AAPL", "GOOG", "MSFT"), tickersStore.readTickers())
    }

    @Test
    fun overwriteReplacesPreviousSet() {
        tickersStore.saveTickers(setOf("AAPL"))
        tickersStore.saveTickers(setOf("GOOG", "MSFT"))
        assertEquals(setOf("GOOG", "MSFT"), tickersStore.readTickers())
    }
}

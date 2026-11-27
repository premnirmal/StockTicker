package com.github.premnirmal.ticker.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.TestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.io.InputStream
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [28])
class WidgetGlanceStateTest {

    @Test
    fun corruptStateIsReplacedAndUpdatesArePersisted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val key = UUID.randomUUID().toString()
        val file = WidgetGlanceStateDefinition.getLocation(context, key)
        file.writeBytes(byteArrayOf(0x17, 0x03, 0x03))
        val store = WidgetGlanceStateDefinition.getDataStore(context, key)

        assertEquals(WidgetGlanceState.Default, store.data.first())
        val updated = WidgetGlanceState(
            widgetState = SerializableWidgetState(showCurrency = true, textColor = 123)
        )
        store.updateData { updated }

        assertEquals(updated, file.inputStream().use { WidgetGlanceStateSerializer.readFrom(it) })
    }

    @Test
    fun ioFailuresAreNotTreatedAsCorruption() {
        val input = object : InputStream() {
            override fun read(): Int = throw IOException("Read failed")
        }
        assertThrows(IOException::class.java) {
            runBlocking { WidgetGlanceStateSerializer.readFrom(input) }
        }
    }
}

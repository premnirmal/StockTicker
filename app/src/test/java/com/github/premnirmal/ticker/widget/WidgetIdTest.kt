package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.test.core.app.ApplicationProvider
import com.github.premnirmal.ticker.TestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [28])
class WidgetIdTest {

    @Test
    fun unavailableWidgetIdIsSkipped() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = GlanceAppWidgetManager(context)

        assertNull(manager.getGlanceIdIfPresent(42))
        assertNull(manager.getGlanceIdIfPresent(AppWidgetManager.INVALID_APPWIDGET_ID))
    }

    @Test
    fun registeredWidgetCanStillBeUpdated() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = AppWidgetProviderInfo().apply {
            provider = ComponentName(context, StockWidget::class.java)
        }
        shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(42, info)
        val manager = GlanceAppWidgetManager(context)

        assertEquals(manager.getGlanceIdBy(42), manager.getGlanceIdIfPresent(42))
    }
}

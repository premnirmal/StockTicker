package com.github.premnirmal.ticker.widget

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.glance.state.GlanceStateDefinition
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Glance state that holds both widget configuration and stock quotes.
 */
@Serializable
data class WidgetGlanceState(
    val widgetState: SerializableWidgetState,
    val quotes: List<Quote> = emptyList(),
) {
    companion object {
        val Default = WidgetGlanceState(
            widgetState = SerializableWidgetState(),
            quotes = emptyList()
        )
    }
}

/**
 * Serializable version of [WidgetData.State] that can be stored in Glance state.
 */
@Serializable
data class SerializableWidgetState(
    val boldText: Boolean = false,
    val changeType: SerializableChangeType = SerializableChangeType.Percent,
    val layoutType: SerializableLayoutType = SerializableLayoutType.Animated,
    val fontSize: Float = 12f,
    val showCurrency: Boolean = false,
    val isDarkMode: Boolean = false,
    val sizePref: Int = 0,
    val hideWidgetHeader: Boolean = false,
    @param:DrawableRes
    @get:DrawableRes
    val backgroundResource: Int = 0,
    @param:ColorRes
    @get:ColorRes
    val positiveTextColor: Int = 0,
    @param:ColorRes
    @get:ColorRes
    val negativeTextColor: Int = 0,
    @param:ColorRes
    @get:ColorRes
    val textColor: Int = 0,
    @param:LayoutRes
    @get:LayoutRes
    val stockViewLayout: Int = 0,
    val isRefreshing: Boolean = false,
    val fetchState: SerializableFetchState = SerializableFetchState.NotFetched,
) {
    val singleStockPerRow: Boolean
        get() = sizePref > 0

    fun getChangeColor(change: Float, changeInPercent: Float): Int {
        return if (change < 0f || changeInPercent < 0f) {
            negativeTextColor
        } else {
            positiveTextColor
        }
    }

    companion object {
        /**
         * Convert from WidgetData.State
         */
        fun from(
            state: WidgetData.State,
            fetchState: StocksProvider.FetchState = StocksProvider.FetchState.NotFetched,
            isRefreshing: Boolean = false
        ): SerializableWidgetState {
            return SerializableWidgetState(
                boldText = state.boldText,
                changeType = SerializableChangeType.from(state.changeType),
                layoutType = SerializableLayoutType.from(state.layoutType),
                fontSize = state.fontSize,
                showCurrency = state.showCurrency,
                isDarkMode = state.isDarkMode,
                sizePref = state.sizePref,
                hideWidgetHeader = state.hideWidgetHeader,
                backgroundResource = state.backgroundResource,
                positiveTextColor = state.positiveTextColor,
                negativeTextColor = state.negativeTextColor,
                textColor = state.textColor,
                stockViewLayout = state.stockViewLayout,
                isRefreshing = isRefreshing,
                fetchState = SerializableFetchState.from(fetchState)
            )
        }
    }
}

@Serializable
enum class SerializableChangeType {
    Value,
    Percent;

    fun toChangeType(): IWidgetData.ChangeType {
        return when (this) {
            Value -> IWidgetData.ChangeType.Value
            Percent -> IWidgetData.ChangeType.Percent
        }
    }

    companion object {
        fun from(changeType: IWidgetData.ChangeType): SerializableChangeType {
            return when (changeType) {
                IWidgetData.ChangeType.Value -> Value
                IWidgetData.ChangeType.Percent -> Percent
            }
        }
    }
}

@Serializable
enum class SerializableLayoutType {
    Animated,
    Tabs,
    Fixed,
    MyPortfolio;

    fun toLayoutType(): IWidgetData.LayoutType {
        return when (this) {
            Animated -> IWidgetData.LayoutType.Animated
            Tabs -> IWidgetData.LayoutType.Tabs
            Fixed -> IWidgetData.LayoutType.Fixed
            MyPortfolio -> IWidgetData.LayoutType.MyPortfolio
        }
    }

    companion object {
        fun from(layoutType: IWidgetData.LayoutType): SerializableLayoutType {
            return when (layoutType) {
                IWidgetData.LayoutType.Animated -> Animated
                IWidgetData.LayoutType.Tabs -> Tabs
                IWidgetData.LayoutType.Fixed -> Fixed
                IWidgetData.LayoutType.MyPortfolio -> MyPortfolio
            }
        }
    }
}

@Serializable
sealed class SerializableFetchState {
    abstract val displayString: String
    @Serializable
    object NotFetched : SerializableFetchState() {
        override val displayString: String = "--"
    }

    @Serializable
    data class Success(val fetchTime: Long) : SerializableFetchState() {

        override val displayString: String by lazy {
            val instant = Instant.ofEpochMilli(fetchTime)
            val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
            time.createTimeString()
        }
    }

    @Serializable
    data class Failure(val errorMessage: String) : SerializableFetchState() {

        override val displayString = errorMessage
    }

    fun toFetchState(): StocksProvider.FetchState {
        return when (this) {
            is NotFetched -> StocksProvider.FetchState.NotFetched
            is Success -> StocksProvider.FetchState.Success(fetchTime)
            is Failure -> StocksProvider.FetchState.Failure(Exception(errorMessage))
        }
    }

    companion object {
        fun from(fetchState: StocksProvider.FetchState): SerializableFetchState {
            return when (fetchState) {
                is StocksProvider.FetchState.NotFetched -> NotFetched
                is StocksProvider.FetchState.Success -> Success(fetchState.fetchTime)
                is StocksProvider.FetchState.Failure -> Failure(fetchState.exception.message.orEmpty())
            }
        }
    }
}

/**
 * Serializer for WidgetGlanceState using JSON
 */
object WidgetGlanceStateSerializer : Serializer<WidgetGlanceState> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: WidgetGlanceState
        get() = WidgetGlanceState.Default

    override suspend fun readFrom(input: InputStream): WidgetGlanceState {
        return try {
            json.decodeFromString(
                WidgetGlanceState.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read widget state", e)
        }
    }

    override suspend fun writeTo(t: WidgetGlanceState, output: OutputStream) {
        output.write(
            json.encodeToString(WidgetGlanceState.serializer(), t).encodeToByteArray()
        )
    }
}

/**
 * [GlanceStateDefinition] for [WidgetGlanceState].
 */
object WidgetGlanceStateDefinition : GlanceStateDefinition<WidgetGlanceState> {
    private const val WIDGET_STATE_FILE = "widget_glance_state"

    override suspend fun getDataStore(
        context: Context,
        fileKey: String
    ): DataStore<WidgetGlanceState> {
        return androidx.datastore.core.DataStoreFactory.create(
            serializer = WidgetGlanceStateSerializer,
            produceFile = {
                File(context.filesDir, "${WIDGET_STATE_FILE}_$fileKey")
            }
        )
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "${WIDGET_STATE_FILE}_$fileKey")
    }
}

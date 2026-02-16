package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.AppPreferences.Companion.toCommaSeparatedString
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.IWidgetData.LayoutType
import com.github.premnirmal.tickerwidget.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.collections.sortByDescending

class WidgetData : IWidgetData {

    companion object {
        private const val SORTED_STOCK_LIST = AppPreferences.SORTED_STOCK_LIST
        private const val PREFS_NAME_PREFIX = "stocks_widget_"
        private const val WIDGET_NAME = "WIDGET_NAME"
        private const val LAYOUT_TYPE = AppPreferences.LAYOUT_TYPE
        private const val WIDGET_SIZE = AppPreferences.WIDGET_SIZE
        private const val BOLD_CHANGE = AppPreferences.BOLD_CHANGE
        private const val SHOW_CURRENCY = AppPreferences.SHOW_CURRENCY
        private const val PERCENT = AppPreferences.PERCENT
        private const val AUTOSORT = AppPreferences.SETTING_AUTOSORT
        private const val HIDE_HEADER = AppPreferences.SETTING_HIDE_HEADER
        private const val WIDGET_BG = AppPreferences.WIDGET_BG
        private const val TEXT_COLOR = AppPreferences.TEXT_COLOR
        private const val TRANSPARENT = AppPreferences.TRANSPARENT
        private const val TRANSLUCENT = AppPreferences.TRANSLUCENT
        private const val SYSTEM = AppPreferences.SYSTEM
        private const val DARK = AppPreferences.DARK
        private const val LIGHT = AppPreferences.LIGHT
    }

    @Inject internal lateinit var stocksProvider: StocksProvider

    @Inject @ApplicationContext
    internal lateinit var context: Context

    @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

    @Inject internal lateinit var appPreferences: AppPreferences

    @Inject internal lateinit var coroutineScope: CoroutineScope

    private val position: Int
    override val widgetId: Int
    private val tickerList: MutableList<String>
    val tickers: StateFlow<List<String>>
        get() = _tickerList
    private val _tickerList = MutableStateFlow<List<String>>(emptyList())
    override val stocks: StateFlow<List<Quote>>
        get() = _stocks
    private val _stocks = MutableStateFlow<List<Quote>>(emptyList())
    private val preferences: SharedPreferences
    private val _autoSortEnabled = MutableStateFlow(false)
    val changeFlow: StateFlow<Prefs>
        get() = _changeFlow
    private val _changeFlow by lazy { MutableStateFlow(toImmutableData()) }

    override val data: StateFlow<State>
        get() = _data
    private val _data by lazy { MutableStateFlow(toState()) }

    constructor(
        position: Int,
        widgetId: Int
    ) {
        this.position = position
        this.widgetId = widgetId
        Injector.appComponent()
            .inject(this)
        val prefsName = "$PREFS_NAME_PREFIX$widgetId"
        preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val tickerListVars = preferences.getString(SORTED_STOCK_LIST, "")
        tickerList = if (tickerListVars.isNullOrEmpty()) {
            ArrayList()
        } else {
            ArrayList(
                listOf(
                    *tickerListVars.split(",".toRegex())
                        .dropLastWhile(String::isEmpty)
                        .toTypedArray()
                )
            )
        }
        save()
        _autoSortEnabled.value = autoSortEnabled()
    }

    constructor(
        position: Int,
        widgetId: Int,
        isFirstWidget: Boolean
    ) : this(position, widgetId) {
        if (isFirstWidget && tickerList.isEmpty()) {
            addAllFromStocksProvider()
        }
    }

    private val nightMode: Boolean
        get() = appPreferences.nightMode == AppCompatDelegate.MODE_NIGHT_YES

    @get:ColorRes
    val positiveTextColor: Int
        get() {
            return when (textColorPref()) {
                SYSTEM -> {
                    if (nightMode) {
                        R.color.text_widget_positive_light
                    } else {
                        R.color.text_widget_positive
                    }
                }

                DARK -> {
                    R.color.text_widget_positive_dark
                }

                LIGHT -> {
                    R.color.text_widget_positive_light
                }

                else -> {
                    R.color.text_widget_positive
                }
            }
        }

    @get:ColorRes
    val negativeTextColor: Int
        get() {
            return R.color.text_widget_negative
        }

    override val widgetName: String
        get() {
            var name = preferences.getString(WIDGET_NAME, "")!!
            if (name.isEmpty()) {
                name = "Widget #$position"
                setWidgetName(name)
            }
            return name
        }

    fun widgetName(): String = widgetName

    fun setWidgetName(value: String) {
        preferences.edit {
            putString(WIDGET_NAME, value)
        }
        _changeFlow.value = toImmutableData()
        _data.value = toState()
        emitWidgetChanges()
        widgetDataProvider.refreshWidgetDataList()
    }

    override val changeType: IWidgetData.ChangeType
        get() = changeType()

    fun changeType(): IWidgetData.ChangeType {
        val state = preferences.getBoolean(PERCENT, false)
        return if (state) IWidgetData.ChangeType.Percent else IWidgetData.ChangeType.Value
    }

    fun flipChange() {
        val state = preferences.getBoolean(PERCENT, false)
        setChange(!state)
    }

    fun setChange(percent: Boolean) {
        preferences.edit {
            putBoolean(PERCENT, percent)
        }
        _changeFlow.value = toImmutableData()
        _data.value = toState()
        emitWidgetChanges()
    }

    val singleStockPerRow: Boolean
        get() = widgetSizePref() > 0

    fun widgetSizePref(): Int = preferences.getInt(WIDGET_SIZE, 0)

    fun setWidgetSizePref(value: Int) {
        preferences.edit {
            putInt(WIDGET_SIZE, value)
        }
        _changeFlow.value = toImmutableData()
        _data.value = toState()
        emitWidgetChanges()
    }

    fun readFontSize(): Float {
        val size = appPreferences.textSizePref
        val resId = when (size) {
            -2 -> R.integer.text_size_nano
            -1 -> R.integer.text_size_mini
            0 -> R.integer.text_size_small
            1 -> R.integer.text_size_medium
            2 -> R.integer.text_size_large
            3 -> R.integer.text_size_huge
            4 -> R.integer.text_size_giant
            else -> R.integer.text_size_medium
        }
        return context.resources.getInteger(resId).toFloat() - 4f
    }

    override fun getChangeColor(context: Context, change: Float, changeInPercent: Float): Color {
        return if (change < 0f || changeInPercent < 0f) {
            Color(ContextCompat.getColor(context, negativeTextColor))
        } else {
            Color(ContextCompat.getColor(context, positiveTextColor))
        }
    }

    fun layoutPref(): Int = preferences.getInt(LAYOUT_TYPE, 0)

    fun setLayoutPref(value: Int) {
        preferences.edit {
            putInt(LAYOUT_TYPE, value)
        }
        _changeFlow.value = toImmutableData()
        _data.value = toState()
        emitWidgetChanges()
    }

    @ColorInt fun textColor(): Int {
        return ContextCompat.getColor(context, textColorRes())
    }

    @ColorRes fun textColorRes(): Int {
        val pref = textColorPref()
        return if (pref == SYSTEM) {
            if (nightMode) {
                R.color.dark_widget_text
            } else {
                R.color.widget_text
            }
        } else if (pref == DARK) {
            R.color.widget_text_black
        } else if (pref == LIGHT) {
            R.color.widget_text_white
        } else {
            R.color.widget_text
        }
    }

    override val layoutType: LayoutType
        get() = LayoutType.fromInt(layoutPref())

    @LayoutRes fun stockViewLayout(): Int {
        return when (layoutPref()) {
            0 -> R.layout.stockview
            1 -> R.layout.stockview2
            2 -> R.layout.stockview3
            else -> R.layout.stockview4
        }
    }

    @DrawableRes
    fun backgroundResource(): Int {
        return when {
            bgPref() == TRANSPARENT -> {
                R.drawable.transparent_widget_bg
            }
            bgPref() == TRANSLUCENT -> {
                R.drawable.translucent_widget_bg
            }
            nightMode -> {
                R.drawable.app_widget_background_dark
            }
            else -> {
                R.drawable.app_widget_background
            }
        }
    }

    fun textColorPref(): Int = preferences.getInt(TEXT_COLOR, SYSTEM)

    fun setTextColorPref(pref: Int) {
        preferences.edit {
            putInt(TEXT_COLOR, pref)
        }
        _changeFlow.value = toImmutableData()
        _data.value = toState()
        emitWidgetChanges()
    }

    fun bgPref(): Int {
        var pref = preferences.getInt(WIDGET_BG, SYSTEM)
        if (pref > TRANSLUCENT) {
            pref = SYSTEM
            setBgPref(pref)
        }
        return pref
    }

    fun setBgPref(value: Int) {
        preferences.edit {
            putInt(WIDGET_BG, value)
        }
        _changeFlow.value = toImmutableData()
        _data.value = toState()
        emitWidgetChanges()
    }

    fun autoSortEnabled(): Boolean = preferences.getBoolean(AUTOSORT, false)

    fun setAutoSort(autoSort: Boolean) {
        preferences.edit {
            putBoolean(AUTOSORT, autoSort)
        }
        _autoSortEnabled.value = autoSort
        save()
    }

    fun readHideHeader(): Boolean = preferences.getBoolean(HIDE_HEADER, false)

    fun setHideHeader(hide: Boolean) {
        preferences.edit {
            putBoolean(HIDE_HEADER, hide)
        }
        _changeFlow.value = toImmutableData()
        _data.value = toState()
        emitWidgetChanges()
    }

    fun readIsBoldEnabled(): Boolean = preferences.getBoolean(BOLD_CHANGE, false)

    fun setBoldEnabled(value: Boolean) {
        preferences.edit {
            putBoolean(BOLD_CHANGE, value)
        }
        _changeFlow.value = toImmutableData()
        _data.value = toState()
        emitWidgetChanges()
    }

    fun readIsCurrencyEnabled(): Boolean = preferences.getBoolean(SHOW_CURRENCY, false)

    fun setCurrencyEnabled(value: Boolean) {
        preferences.edit {
            putBoolean(SHOW_CURRENCY, value)
        }
        _changeFlow.value = toImmutableData()
        _data.value = toState()
        emitWidgetChanges()
    }

    @Deprecated("Use the stocks flow")
    fun getQuotesList(): List<Quote> {
        val quoteList = ArrayList<Quote>()
        tickerList.map { stocksProvider.getStock(it) }
            .forEach { quote -> quote?.let { quoteList.add(it) } }
        if (autoSortEnabled()) {
            quoteList.sortByDescending { it.changeInPercent }
        }
        return quoteList
    }

    fun getTickers(): List<String> = tickerList

    fun hasTicker(symbol: String): Boolean {
        synchronized(tickerList) {
            var found = false
            val toRemove: MutableList<String> = ArrayList()
            for (ticker in tickerList) {
                if (!stocksProvider.hasTicker(ticker)) {
                    toRemove.add(ticker)
                } else {
                    if (ticker == symbol) {
                        found = true
                    }
                }
            }
            tickerList.removeAll(toRemove)
            return found
        }
    }

    fun rearrange(tickers: List<String>) {
        synchronized(tickerList) {
            tickerList.clear()
            tickerList.addAll(tickers)
        }
        save()
    }

    fun addTicker(ticker: String) {
        synchronized(tickerList) {
            if (!tickerList.contains(ticker)) {
                tickerList.add(ticker)
            }
            stocksProvider.addStock(ticker)
        }
        save()
    }

    fun addTickers(tickers: List<String>) {
        synchronized(tickerList) {
            val filtered = tickers.filter { !tickerList.contains(it) }
            tickerList.addAll(filtered)
            stocksProvider.addStocks(filtered.filter { !stocksProvider.hasTicker(it) })
        }
        save()
    }

    fun removeStock(ticker: String) {
        synchronized(tickerList) {
            tickerList.remove(ticker)
        }
        save()
    }

    fun addAllFromStocksProvider() {
        addTickers(stocksProvider.tickers.value)
    }

    fun onWidgetRemoved() {
        preferences.edit {
            clear()
        }
        _changeFlow.value = toImmutableData()
    }

    fun toImmutableData(): Prefs {
        return Prefs(
            id = widgetId,
            name = widgetName(),
            autoSort = autoSortEnabled(),
            boldText = readIsBoldEnabled(),
            hideWidgetHeader = readHideHeader(),
            showCurrency = readIsCurrencyEnabled(),
            isDarkMode = nightMode,
            typePref = layoutPref(),
            sizePref = widgetSizePref(),
            backgroundPref = bgPref(),
            textColourPref = textColorPref(),
            changeType = changeType(),
            layoutType = layoutType,
            fontSize = readFontSize(),
            backgroundResource = backgroundResource(),
            positiveTextColor = positiveTextColor,
            negativeTextColor = negativeTextColor,
            textColor = textColorRes(),
            stockViewLayout = stockViewLayout()
        )
    }

    fun toState(): State {
        return State(
            boldText = readIsBoldEnabled(),
            changeType = changeType(),
            layoutType = layoutType,
            fontSize = readFontSize(),
            showCurrency = readIsCurrencyEnabled(),
            isDarkMode = nightMode,
            sizePref = widgetSizePref(),
            hideWidgetHeader = readHideHeader(),
            backgroundResource = backgroundResource(),
            positiveTextColor = positiveTextColor,
            negativeTextColor = negativeTextColor,
            textColor = textColorRes(),
            stockViewLayout = stockViewLayout()
        )
    }

    private fun save() {
        synchronized(tickerList) {
            preferences.edit { putString(SORTED_STOCK_LIST, tickerList.toCommaSeparatedString()) }
        }
        _changeFlow.value = toImmutableData()
        _data.value = toState()
        _tickerList.value = tickerList
        _stocks.value = tickerList.mapNotNull {
            stocksProvider.getStock(it)
        }.let { quotes ->
            if (autoSortEnabled()) {
                quotes.toMutableList().sortedByDescending { it.changeInPercent }
            } else {
                quotes
            }
        }
        emitWidgetChanges()
    }

    private fun emitWidgetChanges() {
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            coroutineScope.launch {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(widgetId)
                updateAppWidgetState(
                    context = context,
                    definition = WidgetGlanceStateDefinition,
                    glanceId = glanceId,
                ) { state ->
                    val currentQuotes = stocks.value
                    val currentState = data.value
                    val currentFetchState = stocksProvider.fetchState.value
                    val currentIsRefreshing = appPreferences.isRefreshing.value
                    state.copy(
                        widgetState = SerializableWidgetState.from(
                            state = currentState, fetchState = currentFetchState, isRefreshing = currentIsRefreshing
                        ), quotes = currentQuotes
                    )
                }
                GlanceStocksWidget().update(context, glanceId)
            }
        }
    }

    fun refreshStocksList() {
        _tickerList.value = tickerList
        _stocks.value = tickerList.mapNotNull {
            stocksProvider.getStock(it)
        }.let { quotes ->
            if (autoSortEnabled()) {
                quotes.toMutableList().sortedByDescending { it.changeInPercent }
            } else {
                quotes
            }
        }
    }

    override fun fetchStocks() {
        coroutineScope.launch {
            appPreferences.setRefreshing(true)
            val fetchTask = async {
                stocksProvider.fetch()
            }
            fetchTask.await()
            refreshStocksList()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WidgetData) return false

        if (toImmutableData() != other.toImmutableData()) return false
        return true
    }

    override fun hashCode(): Int {
        return toImmutableData().hashCode()
    }

    @Parcelize
    data class State(
        val boldText: Boolean,
        val changeType: IWidgetData.ChangeType,
        val layoutType: LayoutType,
        val fontSize: Float,
        val showCurrency: Boolean,
        val isDarkMode: Boolean,
        val sizePref: Int,
        val hideWidgetHeader: Boolean,
        @param:DrawableRes
        @get:DrawableRes
        val backgroundResource: Int,
        @param:ColorRes
        @get:ColorRes
        val positiveTextColor: Int,
        @param:ColorRes
        @get:ColorRes
        val negativeTextColor: Int,
        @param:ColorRes
        @get:ColorRes
        val textColor: Int,
        @param:LayoutRes
        @get:LayoutRes
        val stockViewLayout: Int,
    ) : Parcelable {

        val singleStockPerRow: Boolean
            get() = sizePref > 0

        @ColorRes
        fun getChangeColor(change: Float, changeInPercent: Float): Int {
            return if (change < 0f || changeInPercent < 0f) {
                negativeTextColor
            } else {
                positiveTextColor
            }
        }
    }

    @Parcelize
    data class Prefs(
        val id: Int,
        val name: String,
        val autoSort: Boolean,
        val boldText: Boolean,
        val hideWidgetHeader: Boolean,
        val showCurrency: Boolean,
        val isDarkMode: Boolean,
        val typePref: Int,
        val sizePref: Int,
        val backgroundPref: Int,
        val textColourPref: Int,
        val changeType: IWidgetData.ChangeType,
        val layoutType: LayoutType,
        val fontSize: Float,
        @param:DrawableRes
        @get:DrawableRes
        val backgroundResource: Int,
        @param:ColorRes
        @get:ColorRes
        val positiveTextColor: Int,
        @param:ColorRes
        @get:ColorRes
        val negativeTextColor: Int,
        @param:ColorRes
        @get:ColorRes
        val textColor: Int,
        @param:LayoutRes
        @get:LayoutRes
        val stockViewLayout: Int,
    ) : Parcelable
}

package com.github.premnirmal.ticker.settings

import com.github.premnirmal.ticker.AppPreferences.Companion.toCommaSeparatedString
import com.github.premnirmal.ticker.widget.WidgetData

class ExportWidget() {
    lateinit var name: String
    lateinit var tickerList: String  // csv list of tickers

    lateinit var autoSort: String
    lateinit var hideHeader: String
    lateinit var showCurrency: String
    lateinit var showBold: String

    lateinit var widgetSize: String   //
    lateinit var layout: String       //
    lateinit var textColor: String
    lateinit var background: String

    constructor(widgetData: WidgetData) : this() {
        name = widgetData.widgetName()
        tickerList = widgetData.getTickers().toCommaSeparatedString()
        autoSort = widgetData.autoSortEnabled.value.toString()
        showCurrency = widgetData.isCurrencyEnabled().toString()
        showBold = widgetData.isBoldEnabled().toString()
        hideHeader = widgetData.hideHeader().toString()
        widgetSize = widgetData.widgetSizePref().toString() // either 0 (default) or 1 (1 ticker per row)
        layout = widgetData.layoutPref().toString()         // 0 Anim,1 Tabs,2 Fixed,3(?)Myportfolio
        textColor = widgetData.textColorPref().toString()   // 0 Sys, 1 Light,2 Dark
        background = widgetData.bgPref().toString()         // 0 Sys, 1, Transparent, 2 Translucent
    }
}

package com.github.premnirmal.ticker.portfolio

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.daimajia.swipe.SwipeLayout
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.ticker.ui.StockFieldView
import com.github.premnirmal.tickerwidget.R

/**
 * Created by premnirmal on 2/29/16.
 */
internal class StockVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun update(stock: Stock?, listener: StocksAdapter.OnStockClickListener) {
        if (stock == null) {
            return
        }

        val position = adapterPosition
        itemView.findViewById(R.id.stockContainer).setOnClickListener { listener.onClick(stock) }
        itemView.findViewById(R.id.trash).setOnClickListener { v -> listener.onRemoveClick(v, stock, position) }

        val swipeLayout = itemView as SwipeLayout
        swipeLayout.showMode = SwipeLayout.ShowMode.PullOut
        swipeLayout.dragEdge = SwipeLayout.DragEdge.Right

        setText(itemView, R.id.ticker, stock.symbol)

        val change: Double
        if (stock != null && stock.Change != null) {
            change = java.lang.Double.parseDouble(stock.Change.replace("+", ""))
        } else {
            change = 0.0
        }

        setText(itemView, R.id.name, stock.Name)

        val changeVal: Double
        val changePercentVal: Double
        if (stock.Change != null) {
            changeVal = java.lang.Double.parseDouble(stock.Change.replace("+", ""))
            changePercentVal = java.lang.Double.parseDouble(stock.ChangeinPercent.replace("+", "").replace("%", ""))
        } else {
            changeVal = 0.0
            changePercentVal = 0.0
        }

        val changeInPercent = itemView.findViewById(R.id.changePercent) as StockFieldView
        changeInPercent.setText(Tools.DECIMAL_FORMAT.format(changePercentVal))
        val changeValue = itemView.findViewById(R.id.changeValue) as StockFieldView
        changeValue.setText(Tools.DECIMAL_FORMAT.format(changeVal))
        val totalValue: TextView = itemView.findViewById(R.id.totalValueText) as TextView
        totalValue.text = Tools.DECIMAL_FORMAT.format(stock.LastTradePriceOnly)

        val color: Int
        if (change >= 0) {
            color = itemView.getResources().getColor(R.color.positive_green)
        } else {
            color = itemView.getResources().getColor(R.color.negative_red)
        }

        changeInPercent.setTextColor(color)
        changeValue.setTextColor(color)

        if (stock.IsPosition == true) {
            setStockFieldLabel(itemView, R.id.averageDailyVolume, "Holdings")
            setStockFieldText(itemView, R.id.averageDailyVolume, Tools.DECIMAL_FORMAT.format(stock.LastTradePriceOnly * stock.PositionShares))

            setStockFieldLabel(itemView, R.id.exchange, "Gain/Loss")
            setStockFieldText(itemView, R.id.exchange, Tools.DECIMAL_FORMAT.format(stock.LastTradePriceOnly * stock.PositionShares - stock.PositionShares * stock.PositionPrice))

            setStockFieldLabel(itemView, R.id.yearHigh, "Change %")
            setStockFieldText(itemView, R.id.yearHigh, "${(Tools.DECIMAL_FORMAT.format((stock.LastTradePriceOnly - stock.PositionPrice) / stock.PositionPrice * 100))}%")

            setStockFieldLabel(itemView, R.id.yearLow, "Change")
            setStockFieldText(itemView, R.id.yearLow, Tools.DECIMAL_FORMAT.format(stock.LastTradePriceOnly - stock.PositionPrice))
        } else {
            setStockFieldLabel(itemView, R.id.averageDailyVolume, "Daily Volume")
            setStockFieldText(itemView, R.id.averageDailyVolume, "${stock.AverageDailyVolume}")
            setStockFieldLabel(itemView, R.id.exchange, "Exchange")
            setStockFieldText(itemView, R.id.exchange, "${stock.StockExchange}")
            setStockFieldLabel(itemView, R.id.yearHigh, "Year High")
            setStockFieldText(itemView, R.id.yearHigh, Tools.DECIMAL_FORMAT.format(stock.YearHigh))
            setStockFieldLabel(itemView, R.id.yearLow, "Year Low")
            setStockFieldText(itemView, R.id.yearLow, Tools.DECIMAL_FORMAT.format(stock.YearLow))
        }

        val padding = itemView.getResources().getDimension(R.dimen.text_padding).toInt()
        itemView.setPadding(0, padding, 0, padding)
    }

    companion object {

        fun setText(parent: View, textViewId: Int, text: CharSequence?) {
            val textView = parent.findViewById(textViewId) as TextView
            textView.text = text
        }

        fun setStockFieldLabel(parent: View, textViewId: Int, text: CharSequence) {
            val textView = parent.findViewById(textViewId) as StockFieldView
            textView.setLabel(text)
        }

        fun setStockFieldText(parent: View, textViewId: Int, text: CharSequence) {
            val textView = parent.findViewById(textViewId) as StockFieldView
            textView.setText(text)
        }
    }
}

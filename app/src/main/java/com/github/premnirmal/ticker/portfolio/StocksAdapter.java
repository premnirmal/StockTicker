package com.github.premnirmal.ticker.portfolio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.ticker.ui.StockFieldView;
import com.github.premnirmal.tickerwidget.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by premnirmal on 12/21/14.
 */
class StocksAdapter extends BaseAdapter {

    private final List<Stock> stockList;
    private final OnRemoveClickListener listener;

    interface OnRemoveClickListener {
        void onRemoveClick(View view, Stock stock, int position);
    }

    StocksAdapter(IStocksProvider stocksProvider, OnRemoveClickListener listener) {
        this.listener = listener;
        stockList = stocksProvider.getStocks() == null
                ? new ArrayList<Stock>()
                : new ArrayList<>(stocksProvider.getStocks());
    }

    boolean remove(final Stock stock) {
        return stockList.remove(stock);
    }

    void refresh(IStocksProvider stocksProvider) {
        stockList.clear();
        stockList.addAll(stocksProvider.getStocks() == null
                ? new ArrayList<Stock>()
                : stocksProvider.getStocks());
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return stockList.size();
    }

    @Override
    public Stock getItem(int position) {
        return stockList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        final Stock stock = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.portfolio_item_view, null);
        }

        convertView.findViewById(R.id.trash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRemoveClick(v, stock, position);
            }
        });

        final SwipeLayout swipeLayout = (SwipeLayout) convertView;
        swipeLayout.setShowMode(SwipeLayout.ShowMode.PullOut);
        swipeLayout.setDragEdge(position % 2 == 0 ? SwipeLayout.DragEdge.Left : SwipeLayout.DragEdge.Right);

        setText(convertView, R.id.ticker, stock.symbol);

        final double change;
        if (stock != null && stock.Change != null) {
            change = Double.parseDouble(stock.Change.replace("+", ""));
        } else {
            change = 0d;
        }

        setText(convertView, R.id.name, stock.Name);

        final StockFieldView changeInPercent = (StockFieldView) convertView.findViewById(R.id.changePercent);
        changeInPercent.setText(stock.ChangeinPercent);
        final StockFieldView changeValue = (StockFieldView) convertView.findViewById(R.id.changeValue);
        changeValue.setText(stock.Change);
        setStockFieldText(convertView, R.id.totalValue, String.valueOf(stock.LastTradePriceOnly));

        final int color;
        if (change >= 0) {
            color = context.getResources().getColor(R.color.positive_green);
        } else {
            color = context.getResources().getColor(R.color.negative_red);
        }

        changeInPercent.setTextColor(color);
        changeValue.setTextColor(color);

        if (stock.IsPosition == true) {
            setStockFieldLabel(convertView, R.id.averageDailyVolume, "Value");
            setStockFieldText(convertView, R.id.averageDailyVolume, String.format("%.2f", stock.LastTradePriceOnly * stock.PositionShares));

            setStockFieldLabel(convertView, R.id.exchange, "Gain/Loss");
            setStockFieldText(convertView, R.id.exchange, String.format("%.2f", (stock.LastTradePriceOnly * stock.PositionShares) - (stock.PositionShares * stock.PositionPrice)));
 
            setStockFieldLabel(convertView, R.id.yearHigh, "Change %");
            setStockFieldText(convertView, R.id.yearHigh, String.format("%.2f",((stock.LastTradePriceOnly - stock.PositionPrice) / stock.PositionPrice) * 100));

            setStockFieldLabel(convertView, R.id.yearLow, "Change");
            setStockFieldText(convertView, R.id.yearLow, String.format("%.2f",stock.LastTradePriceOnly-stock.PositionPrice));
        } else {
            setStockFieldLabel(convertView, R.id.averageDailyVolume, "Daily Volume");
            setStockFieldText(convertView, R.id.averageDailyVolume, String.valueOf(stock.AverageDailyVolume));
            setStockFieldLabel(convertView, R.id.exchange, "Exchange");
            setStockFieldText(convertView, R.id.exchange, String.valueOf(stock.StockExchange));
            setStockFieldLabel(convertView, R.id.yearHigh, "Year High");
            setStockFieldText(convertView, R.id.yearHigh, String.valueOf(stock.YearHigh));
            setStockFieldLabel(convertView, R.id.yearLow, "Year Low");
            setStockFieldText(convertView, R.id.yearLow, String.valueOf(stock.YearLow));
        }

        final int padding = (int) context.getResources().getDimension(R.dimen.text_padding);
        convertView.setPadding(0, padding, 0, padding);

        return convertView;
    }

    static void setText(View parent, int textViewId, CharSequence text) {
        final TextView textView = (TextView) parent.findViewById(textViewId);
        textView.setText(text);
    }

    static void setStockFieldLabel(View parent, int textViewId, CharSequence text) {
        final StockFieldView textView = (StockFieldView) parent.findViewById(textViewId);
        textView.setLabel(text);
    }

    static void setStockFieldText(View parent, int textViewId, CharSequence text) {
        final StockFieldView textView = (StockFieldView) parent.findViewById(textViewId);
        textView.setText(text);
    }
}

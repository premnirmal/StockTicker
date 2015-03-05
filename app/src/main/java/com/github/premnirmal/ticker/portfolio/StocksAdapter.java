package com.github.premnirmal.ticker.portfolio;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.premnirmal.ticker.ui.StockFieldView;
import com.github.premnirmal.tickerwidget.R;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Stock;
import com.terlici.dragndroplist.DragNDropAdapter;
import com.terlici.dragndroplist.DragNDropListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by premnirmal on 12/21/14.
 */
class StocksAdapter extends BaseAdapter implements DragNDropAdapter {

    private final List<Stock> stockList;
    private final IStocksProvider stocksProvider;
    private final boolean autoSort;

    StocksAdapter(IStocksProvider stocksProvider, boolean autoSort) {
        this.stocksProvider = stocksProvider;
        this.autoSort = autoSort;
        stockList = stocksProvider.getStocks() == null ? new ArrayList<Stock>()
                : new ArrayList<>(stocksProvider.getStocks());
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
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        final Stock stock = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.portfolio_item_view, null);
        }

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
        setStockFieldText(convertView, R.id.averageDailyVolume, String.valueOf(stock.AverageDailyVolume));
        setStockFieldText(convertView, R.id.exchange, String.valueOf(stock.StockExchange));
        setStockFieldText(convertView, R.id.yearHigh, String.valueOf(stock.YearHigh));
        setStockFieldText(convertView, R.id.yearLow, String.valueOf(stock.YearLow));

        final int color;
        if (change >= 0) {
            color = Color.GREEN;
        } else {
            color = Color.RED;
        }

        changeInPercent.setTextColor(color);
        changeValue.setTextColor(color);

        final int padding = (int) context.getResources().getDimension(R.dimen.text_padding);
        convertView.setPadding(0, padding, 0, padding);

        final View dragHandler = convertView.findViewById(R.id.dragHandler);
        dragHandler.setVisibility(autoSort ? View.GONE : View.VISIBLE);

        return convertView;
    }

    @Override
    public int getDragHandler() {
        return R.id.dragHandler;
    }

    @Override
    public void onItemDrag(DragNDropListView parent, View view, int position, long id) {

    }

    @Override
    public void onItemDrop(DragNDropListView parent, View view, int startPosition, int endPosition, long id) {
        stockList.add(endPosition, stockList.remove(startPosition));
        final List<String> newTickerList = new ArrayList<>();
        for (Stock stock : stockList) {
            newTickerList.add(stock.symbol);
        }
        stocksProvider.rearrange(newTickerList);
        notifyDataSetChanged();
    }

    static void setText(View parent, int textViewId, CharSequence text) {
        final TextView textView = (TextView) parent.findViewById(textViewId);
        textView.setText(text);
    }

    static void setStockFieldText(View parent, int textViewId, CharSequence text) {
        final StockFieldView textView = (StockFieldView) parent.findViewById(textViewId);
        textView.setText(text);
    }
}

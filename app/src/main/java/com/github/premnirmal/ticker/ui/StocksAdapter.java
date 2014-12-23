package com.github.premnirmal.ticker.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.premnirmal.ticker.R;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Stock;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by premnirmal on 12/21/14.
 */
class StocksAdapter extends BaseAdapter {

    final List<Stock> stockList;

    StocksAdapter(IStocksProvider stocksProvider) {
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
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        final Stock stock = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.stockview_activity, null);
        }
        final TextView symbol = (TextView) convertView.findViewById(R.id.ticker);
        symbol.setText(stock.symbol);

        final double change;
        if (stock != null && stock.Change != null) {
            change = Double.parseDouble(stock.Change.replace("+", ""));
        } else {
            change = 0d;
        }

        final TextView changeInPercent = (TextView) convertView.findViewById(R.id.changePercent);
        changeInPercent.setText(stock.ChangeinPercent);
        final TextView changeValue = (TextView) convertView.findViewById(R.id.changeValue);
        changeValue.setText(stock.Change);
        final TextView totalValue = (TextView) convertView.findViewById(R.id.totalValue);
        totalValue.setText(String.valueOf(stock.LastTradePriceOnly));

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

        return convertView;
    }
}

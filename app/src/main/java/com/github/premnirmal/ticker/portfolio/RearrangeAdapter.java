package com.github.premnirmal.ticker.portfolio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.tickerwidget.R;
import com.nhaarman.listviewanimations.util.Swappable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by premnirmal on 3/24/15.
 */
public class RearrangeAdapter extends BaseAdapter implements Swappable {

    private final List<Stock> stockList;
    private final IStocksProvider stocksProvider;

    RearrangeAdapter(IStocksProvider stocksProvider) {
        this.stocksProvider = stocksProvider;
        stockList = new ArrayList<>(this.stocksProvider.getStocks());
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
        return getItem(position).symbol.hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.rearrange_view,null);
        }
        final Stock stock = getItem(position);
        ((TextView) convertView.findViewById(R.id.tickerName)).setText(stock.symbol + "\n(" + stock.Name + ")");
        return convertView;
    }

    @Override
    public void swapItems(int from, int to) {
        stockList.add(to, stockList.remove(from));
        final List<String> newTickerList = new ArrayList<>();
        for (Stock stock : stockList) {
            newTickerList.add(stock.symbol);
        }
        notifyDataSetChanged();
        stocksProvider.rearrange(newTickerList);
    }
}

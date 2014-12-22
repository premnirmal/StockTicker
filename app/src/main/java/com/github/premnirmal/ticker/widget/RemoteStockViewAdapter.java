package com.github.premnirmal.ticker.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.github.premnirmal.ticker.R;
import com.github.premnirmal.ticker.StocksApp;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Stock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by premnirmal on 12/21/14.
 */
public class RemoteStockViewAdapter implements RemoteViewsService.RemoteViewsFactory {

    private List<Stock> stocks;
    private Context context;

    @Inject
    IStocksProvider stocksProvider;

    public RemoteStockViewAdapter(Context context) {
        ((StocksApp) context.getApplicationContext()).inject(this);
        Collection<Stock> stocks = stocksProvider.getStocks();
        this.stocks = stocks == null ? new ArrayList<Stock>() : new ArrayList<Stock>(stocks);
        this.context = context;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        Collection<Stock> stocks = stocksProvider.getStocks();
        this.stocks = stocks == null ? new ArrayList<Stock>() : new ArrayList<Stock>(stocks);
    }

    @Override
    public void onDestroy() {
        context = null;
    }

    @Override
    public int getCount() {
        return stocks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.stockview);
        final Stock stock = stocks.get(position);
        remoteViews.setTextViewText(R.id.ticker, stock.symbol);
        remoteViews.setTextViewText(R.id.changePercent, stock.ChangeinPercent);
        remoteViews.setTextViewText(R.id.changeValue, stock.Change);
        remoteViews.setTextViewText(R.id.totalValue, String.valueOf(stock.LastTradePriceOnly));

        final double change;
        if (stock != null && stock.Change != null) {
            change = Double.parseDouble(stock.Change.replace("+", "").replace("-", ""));
        } else {
            change = 0d;
        }


        final int color;
        if (change > 0) {
            color = Color.GREEN;
        } else if (change < 0) {
            color = Color.RED;
        } else {
            color = Color.WHITE;
        }

        remoteViews.setTextColor(R.id.changePercent, color);
        remoteViews.setTextColor(R.id.changeValue, color);

        final float fontSize = Tools.getFontSize(context);
        remoteViews.setTextViewTextSize(R.id.ticker, TypedValue.COMPLEX_UNIT_SP, fontSize);
        remoteViews.setTextViewTextSize(R.id.changePercent, TypedValue.COMPLEX_UNIT_SP, fontSize);
        remoteViews.setTextViewTextSize(R.id.changeValue, TypedValue.COMPLEX_UNIT_SP, fontSize);
        remoteViews.setTextViewTextSize(R.id.totalValue, TypedValue.COMPLEX_UNIT_SP, fontSize);

        final Intent fillInIntent = new Intent();
        remoteViews.setOnClickFillInIntent(R.id.row, fillInIntent);

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(context.getPackageName(), R.layout.loadview);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}

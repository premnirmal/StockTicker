package com.github.premnirmal.ticker.portfolio;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.tickerwidget.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by premnirmal on 12/21/14.
 */
class StocksAdapter extends RecyclerView.Adapter<StockVH> {

    private final List<Stock> stockList;
    private final OnStockClickListener listener;

    interface OnStockClickListener {
        void onRemoveClick(View view, Stock stock, int position);
        void onClick(Stock stock);
    }

    StocksAdapter(IStocksProvider stocksProvider, OnStockClickListener listener) {
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
    public StockVH onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        final View itemView = LayoutInflater.from(context).inflate(R.layout.portfolio_item_view, null);
        return new StockVH(itemView);
    }

    @Override
    public void onBindViewHolder(StockVH holder, int position) {
        holder.update(stockList.get(position), listener);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }
}

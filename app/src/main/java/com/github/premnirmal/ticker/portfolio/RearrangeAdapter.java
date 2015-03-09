package com.github.premnirmal.ticker.portfolio;

import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.tickerwidget.R;
import com.makeramen.dragsortadapter.DragSortAdapter;
import com.makeramen.dragsortadapter.NoForegroundShadowBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by premnirmal on 3/8/15.
 */
class RearrangeAdapter extends DragSortAdapter<RearrangeAdapter.VH> {

    private final List<Stock> stockList;
    private final IStocksProvider stocksProvider;

    RearrangeAdapter(RecyclerView recyclerView, IStocksProvider stocksProvider) {
        super(recyclerView);
        this.stocksProvider = stocksProvider;
        stockList = new ArrayList<>(this.stocksProvider.getStocks());
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.rearrange_view, null));
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.update(position);
    }

    Stock getItem(int position) {
        return stockList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    @Override
    public int getPositionForId(long l) {
        return (int) l;
    }

    @Override
    public boolean move(int from, int to) {
        stockList.add(to, stockList.remove(from));
        final List<String> newTickerList = new ArrayList<>();
        for (Stock stock : stockList) {
            newTickerList.add(stock.symbol);
        }
        notifyDataSetChanged();
        stocksProvider.rearrange(newTickerList);
        return true;
    }

    public class VH extends DragSortAdapter.ViewHolder implements View.OnLongClickListener {

        public VH(View itemView) {
            super(itemView);
            itemView.setOnLongClickListener(this);
        }

        void update(int position) {
            final Stock stock = getItem(position);
            ((TextView) itemView.findViewById(R.id.tickerName)).setText(stock.symbol + "\n(" + stock.Name + ")");
        }

        @Override
        public boolean onLongClick(@NonNull View v) {
            startDrag();
            return true;
        }

        @Override
        public View.DragShadowBuilder getShadowBuilder(View itemView, Point touchPoint) {
            return new NoForegroundShadowBuilder(itemView, touchPoint);
        }
    }
}

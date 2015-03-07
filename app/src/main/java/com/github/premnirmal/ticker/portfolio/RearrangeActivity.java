package com.github.premnirmal.ticker.portfolio;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.tickerwidget.R;
import com.terlici.dragndroplist.DragNDropAdapter;
import com.terlici.dragndroplist.DragNDropListView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by premnirmal on 3/7/15.
 */
public class RearrangeActivity extends BaseActivity {

    @Inject
    IStocksProvider stocksProvider;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.inject(this);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        final DragNDropListView dragNDropListView = new DragNDropListView(this);
        dragNDropListView.setDragNDropAdapter(new RearrangeAdapter());
        setContentView(dragNDropListView);
    }

    public class RearrangeAdapter extends BaseAdapter implements DragNDropAdapter {

        List<Stock> stockList;

        RearrangeAdapter() {
            stockList = new ArrayList<>(stocksProvider.getStocks());
        }

        List<String> reArrange(int from, int to) {
            stockList.add(to, stockList.remove(from));
            final List<String> newTickerList = new ArrayList<>();
            for (Stock stock : stockList) {
                newTickerList.add(stock.symbol);
            }
            notifyDataSetChanged();
            stocksProvider.rearrange(newTickerList);
            return newTickerList;
        }

        @Override
        public int getDragHandler() {
            return R.id.dragHandler;
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
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.rearrange_view, null);
            }
            final Stock stock = getItem(position);
            ((TextView) convertView.findViewById(R.id.tickerName)).setText(stock.symbol + " (" + stock.Name + ")");
            ((ImageView) convertView.findViewById(R.id.dragHandler)).setColorFilter(Color.WHITE);
            return convertView;
        }

        @Override
        public void onItemDrag(DragNDropListView parent, View view, int position, long id) {

        }

        @Override
        public void onItemDrop(DragNDropListView parent, View view, int startPosition, int endPosition, long id) {
            reArrange(startPosition, endPosition);
        }
    }

}

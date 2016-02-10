package com.github.premnirmal.ticker.portfolio;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.ticker.ui.StockFieldView;
import com.github.premnirmal.tickerwidget.R;

/**
 * Created by premnirmal on 2/10/16.
 */
class StockVH extends RecyclerView.ViewHolder{

    StockVH(View itemView) {
        super(itemView);
    }
    
    void update(final Stock stock, final StocksAdapter.OnStockClickListener listener) {
        final int position = getPosition();
        itemView.findViewById(R.id.stockContainer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(stock);
            }
        });
        itemView.findViewById(R.id.trash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRemoveClick(v, stock, position);
            }
        });

        final SwipeLayout swipeLayout = (SwipeLayout) itemView;
        swipeLayout.setShowMode(SwipeLayout.ShowMode.PullOut);
        swipeLayout.setDragEdge(SwipeLayout.DragEdge.Right);

        setText(itemView, R.id.ticker, stock.symbol);

        final double change;
        if (stock != null && stock.Change != null) {
            change = Double.parseDouble(stock.Change.replace("+", ""));
        } else {
            change = 0d;
        }

        setText(itemView, R.id.name, stock.Name);

        final StockFieldView changeInPercent = (StockFieldView) itemView.findViewById(R.id.changePercent);
        changeInPercent.setText(stock.ChangeinPercent);
        final StockFieldView changeValue = (StockFieldView) itemView.findViewById(R.id.changeValue);
        changeValue.setText(stock.Change);
        setStockFieldText(itemView, R.id.totalValue, String.valueOf(stock.LastTradePriceOnly));

        final int color;
        if (change >= 0) {
            color = itemView.getResources().getColor(R.color.positive_green);
        } else {
            color = itemView.getResources().getColor(R.color.negative_red);
        }

        changeInPercent.setTextColor(color);
        changeValue.setTextColor(color);

        if (stock.IsPosition == true) {
            setStockFieldLabel(itemView, R.id.averageDailyVolume, "Value");
            setStockFieldText(itemView, R.id.averageDailyVolume, String.format("%.2f", stock.LastTradePriceOnly * stock.PositionShares));

            setStockFieldLabel(itemView, R.id.exchange, "Gain/Loss");
            setStockFieldText(itemView, R.id.exchange, String.format("%.2f", (stock.LastTradePriceOnly * stock.PositionShares) - (stock.PositionShares * stock.PositionPrice)));

            setStockFieldLabel(itemView, R.id.yearHigh, "Change %");
            setStockFieldText(itemView, R.id.yearHigh, String.format("%.2f",((stock.LastTradePriceOnly - stock.PositionPrice) / stock.PositionPrice) * 100));

            setStockFieldLabel(itemView, R.id.yearLow, "Change");
            setStockFieldText(itemView, R.id.yearLow, String.format("%.2f",stock.LastTradePriceOnly-stock.PositionPrice));
        } else {
            setStockFieldLabel(itemView, R.id.averageDailyVolume, "Daily Volume");
            setStockFieldText(itemView, R.id.averageDailyVolume, String.valueOf(stock.AverageDailyVolume));
            setStockFieldLabel(itemView, R.id.exchange, "Exchange");
            setStockFieldText(itemView, R.id.exchange, String.valueOf(stock.StockExchange));
            setStockFieldLabel(itemView, R.id.yearHigh, "Year High");
            setStockFieldText(itemView, R.id.yearHigh, String.valueOf(stock.YearHigh));
            setStockFieldLabel(itemView, R.id.yearLow, "Year Low");
            setStockFieldText(itemView, R.id.yearLow, String.valueOf(stock.YearLow));
        }

        final int padding = (int) itemView.getResources().getDimension(R.dimen.text_padding);
        itemView.setPadding(0, padding, 0, padding);
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

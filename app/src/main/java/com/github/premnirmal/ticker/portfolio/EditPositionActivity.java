package com.github.premnirmal.ticker.portfolio;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.tickerwidget.R;

/**
 * Created by premnirmal on 7/3/15.
 */
public class EditPositionActivity extends AddPositionActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Stock stock = stocksProvider.getStock(ticker);
        if(stock != null) {
            final EditText sharesView = (EditText) findViewById(R.id.shares);
            final EditText priceView = (EditText) findViewById(R.id.price);
            sharesView.setText("" + stock.PositionShares);
            priceView.setText("" + stock.PositionPrice);
        } else {
            showDialog(getString(R.string.no_such_stock_in_portfolio), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
    }
}

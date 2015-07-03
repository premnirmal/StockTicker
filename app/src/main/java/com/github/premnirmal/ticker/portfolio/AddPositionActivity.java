package com.github.premnirmal.ticker.portfolio;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.tickerwidget.R;

import javax.inject.Inject;

/**
 * Created by premnirmal on 7/3/15.
 */
public class AddPositionActivity extends BaseActivity {

    public static final String TICKER = "TICKER";

    @Inject
    IStocksProvider stocksProvider;
    protected String ticker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.inject(this);
        setContentView(R.layout.activity_positions);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ticker = getIntent().getStringExtra(TICKER);
        final TextView name = (TextView) findViewById(R.id.tickerName);
        name.setText(ticker);

        findViewById(R.id.doneButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDoneClicked();
            }
        });

        findViewById(R.id.skipButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    protected void onDoneClicked() {
        final EditText sharesView = (EditText) findViewById(R.id.shares);
        final EditText priceView = (EditText) findViewById(R.id.price);
        final float price = Float.parseFloat(priceView.getText().toString());
        final float shares = Float.parseFloat(sharesView.getText().toString());
        stocksProvider.addPosition(ticker, shares, price);
        finish();
    }
}

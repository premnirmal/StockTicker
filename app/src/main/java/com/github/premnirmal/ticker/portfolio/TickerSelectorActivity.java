package com.github.premnirmal.ticker.portfolio;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.InAppMessage;
import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Suggestion;
import com.github.premnirmal.ticker.network.SuggestionApi;
import com.github.premnirmal.ticker.network.Suggestions;
import com.github.premnirmal.tickerwidget.R;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by premnirmal on 12/21/14.
 */
public class TickerSelectorActivity extends BaseActivity {

    @Inject
    SuggestionApi suggestionApi;

    @Inject
    IStocksProvider stocksProvider;

    Subscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.inject(this);
        setContentView(R.layout.stock_search_layout);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final EditText searchView = (EditText) findViewById(R.id.query);
        final ListView listView = (ListView) findViewById(R.id.resultList);

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String query = s.toString().trim().replaceAll(" ", "");
                if (!query.isEmpty()) {
                    if (subscription != null) {
                        subscription.unsubscribe();
                    }
                    if (Tools.isNetworkOnline(getApplicationContext())) {
                        final Observable<Suggestions> observable = suggestionApi.getSuggestions(query);
                        subscription = bind(observable)
                                .map(new Func1<Suggestions, List<Suggestion>>() {
                                    @Override
                                    public List<Suggestion> call(Suggestions suggestions) {
                                        return suggestions.ResultSet.Result;
                                    }
                                })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(new Subscriber<List<Suggestion>>() {
                                    @Override
                                    public void onCompleted() {

                                    }

                                    @Override
                                    public void onError(Throwable throwable) {
                                        InAppMessage.showMessage(TickerSelectorActivity.this, R.string.error_fetching_suggestions);
                                    }

                                    @Override
                                    public void onNext(List<Suggestion> suggestions) {
                                        final List<Suggestion> suggestionList = suggestions;
                                        listView.setAdapter(new SuggestionsAdapter(suggestionList));
                                    }
                                });
                    } else {
                        InAppMessage.showMessage(TickerSelectorActivity.this, R.string.no_network_message);
                    }
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final SuggestionsAdapter suggestionsAdapter = (SuggestionsAdapter) parent.getAdapter();
                final Suggestion suggestion = suggestionsAdapter.getItem(position);
                final String ticker = suggestion.symbol;
                if (!stocksProvider.getTickers().contains(ticker)) {
                    stocksProvider.addStock(ticker);
                    InAppMessage.showMessage(TickerSelectorActivity.this, ticker + " added to list");
                    showDialog("Do you want to add positions for " + ticker + "?", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final Intent intent = new Intent(TickerSelectorActivity.this, AddPositionActivity.class);
                            intent.putExtra(EditPositionActivity.TICKER, ticker);
                            startActivity(intent);
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                } else {
                    showDialog(ticker + " is already in your portfolio");
                }

            }
        });

    }


}

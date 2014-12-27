package com.github.premnirmal.ticker.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.premnirmal.tickerwidget.R;
import com.github.premnirmal.ticker.network.Suggestion;

import java.util.List;

/**
* Created by premnirmal on 12/21/14.
*/
class SuggestionsAdapter extends BaseAdapter {

    final List<Suggestion> suggestions;

    SuggestionsAdapter(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public int getCount() {
        return suggestions.size();
    }

    @Override
    public Suggestion getItem(int position) {
        return suggestions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        if(convertView == null) {
            convertView = new TextView(context);
        }

        final Suggestion item = getItem(position);
        ((TextView)convertView).setText(item.symbol + " (" + item.name + ")");
        final int padding = (int) context.getResources().getDimension(R.dimen.text_padding);
        convertView.setPadding(padding,padding,padding,padding);

        return convertView;
    }
}

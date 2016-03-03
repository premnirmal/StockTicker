package com.github.premnirmal.ticker.portfolio

import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.github.premnirmal.ticker.network.Suggestion
import com.github.premnirmal.tickerwidget.R

/**
 * Created by premnirmal on 2/26/16.
 */
internal class SuggestionsAdapter(val suggestions: List<Suggestion>) : BaseAdapter() {

    override fun getCount(): Int {
        return suggestions.size
    }

    override fun getItem(position: Int): Suggestion {
        return suggestions[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val context = parent.context
        val textView: TextView
        if (convertView == null) {
            textView = TextView(context)
        } else {
            textView = convertView as TextView
        }
        val item = getItem(position)
        val name = item.symbol + " - " + Html.fromHtml(item.name) + " (" + item.exchDisp + ")"
        textView.text = name
        val padding = context.resources.getDimension(R.dimen.text_padding).toInt()
        textView.setPadding(padding, padding, padding, padding)

        return textView
    }
}

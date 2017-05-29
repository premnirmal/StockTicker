package com.github.premnirmal.ticker.portfolio.search

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.premnirmal.ticker.network.data.Suggestions.Suggestion
import com.github.premnirmal.ticker.portfolio.search.SuggestionsAdapter.SuggestionVH
import com.github.premnirmal.tickerwidget.R
import java.util.ArrayList

/**
 * Created by premnirmal on 2/26/16.
 */
internal class SuggestionsAdapter(val callback: Callback) : RecyclerView.Adapter<SuggestionVH>() {

  val suggestions: MutableList<Suggestion> = ArrayList()

  interface Callback {
    fun onSuggestionClick(suggestion: Suggestion)
  }

  override fun getItemCount(): Int {
    return suggestions.size
  }

  fun getItem(position: Int): Suggestion {
    return suggestions[position]
  }

  fun setData(data: List<Suggestion>) {
    suggestions.clear()
    suggestions.addAll(data)
    notifyDataSetChanged()
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): SuggestionVH {
    val inflater = LayoutInflater.from(parent?.context)
    return SuggestionVH(inflater.inflate(R.layout.item_suggestion, parent, false), callback)
  }

  override fun onBindViewHolder(holder: SuggestionVH, position: Int) {
    holder.update(getItem(position))
  }

  class SuggestionVH(itemView: View, val callback: Callback) : RecyclerView.ViewHolder(itemView) {

    val textView: TextView = itemView as TextView
    var suggestion: Suggestion? = null

    init {
      itemView.setOnClickListener { _ ->
        if (suggestion != null) {
          callback.onSuggestionClick(suggestion!!)
        }
      }
    }

    fun update(item: Suggestion) {
      suggestion = item
      val name = item.symbol + " - " + Html.fromHtml(item.name) + " (" + item.exchDisp + ")"
      textView.text = name
    }
  }
}

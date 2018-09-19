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
internal class SuggestionsAdapter(private val callback: Callback) : RecyclerView.Adapter<SuggestionVH>() {

  private val suggestions: MutableList<Suggestion> = ArrayList()

  interface Callback {
    fun onSuggestionClick(suggestion: Suggestion)
  }

  override fun getItemCount(): Int {
    return suggestions.size
  }

  private fun getItem(position: Int): Suggestion {
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

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionVH {
    val inflater = LayoutInflater.from(parent.context)
    return SuggestionVH(inflater.inflate(R.layout.item_suggestion, parent, false), callback)
  }

  override fun onBindViewHolder(holder: SuggestionVH, position: Int) {
    holder.update(getItem(position))
  }

  class SuggestionVH(itemView: View, private val callback: Callback) : RecyclerView.ViewHolder(itemView) {

    private val textView: TextView = itemView as TextView
    private var suggestion: Suggestion? = null

    init {
      itemView.setOnClickListener { _ ->
        suggestion?.let { callback.onSuggestionClick(it) }
      }
    }

    fun update(item: Suggestion) {
      suggestion = item
      val builder = StringBuilder(item.symbol)
      if (item.name.isNotBlank()) {
        builder.append(" - ")
        builder.append(Html.fromHtml(item.name))
      }
      if (!item.exchDisp.isBlank()) {
        builder.append(" (")
        builder.append(item.exchDisp)
        builder.append(')')
      }
      val name = builder.toString()
      textView.text = name
    }
  }
}

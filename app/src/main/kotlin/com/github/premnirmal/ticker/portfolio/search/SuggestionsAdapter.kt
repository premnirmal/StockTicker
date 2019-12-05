package com.github.premnirmal.ticker.portfolio.search

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.portfolio.search.SuggestionsAdapter.SuggestionVH
import com.github.premnirmal.tickerwidget.R
import java.util.ArrayList

/**
 * Created by premnirmal on 2/26/16.
 */
internal class SuggestionsAdapter(private val suggestionClickListener: SuggestionClickListener) :
    RecyclerView.Adapter<SuggestionVH>() {

  private val suggestions: MutableList<Suggestion> = ArrayList()

  interface SuggestionClickListener {
    fun onSuggestionClick(suggestion: Suggestion): Boolean
    fun onAddRemoveClick(suggestion: Suggestion): Boolean
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

  fun getData() = suggestions as List<Suggestion>

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): SuggestionVH {
    val inflater = LayoutInflater.from(parent.context)
    return SuggestionVH(
        inflater.inflate(R.layout.item_suggestion, parent, false),
        suggestionClickListener
    )
  }

  override fun onBindViewHolder(
    holder: SuggestionVH,
    position: Int
  ) {
    holder.update(getItem(position))
  }

  class SuggestionVH(
    itemView: View,
    private val suggestionClickListener: SuggestionClickListener
  ) : RecyclerView.ViewHolder(itemView) {

    private val textView: TextView = itemView.findViewById(R.id.suggestion_text)
    private val addRemoveImage: ImageView = itemView.findViewById(R.id.add_remove_image)
    private var suggestion: Suggestion? = null

    init {
      textView.setOnClickListener { _ ->
        suggestion?.let {
          if (suggestionClickListener.onSuggestionClick(it)) {
            it.exists = !it.exists
            update(it)
          }
        }
      }
      addRemoveImage.setOnClickListener {
        suggestion?.let {
          if(suggestionClickListener.onAddRemoveClick(it)) {
            it.exists = !it.exists
            update(it)
          }
        }
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
      addRemoveImage.setImageResource(
          if (item.exists) R.drawable.ic_remove_circle else R.drawable.ic_add_circle
      )
    }
  }
}

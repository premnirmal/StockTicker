package com.github.premnirmal.ticker.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.premnirmal.ticker.news.QuoteDetailViewModel.QuoteDetail
import com.github.premnirmal.ticker.news.QuoteDetailsAdapter.ViewHolder
import com.github.premnirmal.tickerwidget.R

class QuoteDetailsAdapter : ListAdapter<QuoteDetail, ViewHolder>(
    DETAIL_COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.item_quote_details, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.update(getItem(position))
        }
    }

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val title = itemView.findViewById<TextView>(R.id.title)
        private val data = itemView.findViewById<TextView>(R.id.data)

        fun bind(quoteDetail: QuoteDetail) {
            title.setText(quoteDetail.title)
            data.text = quoteDetail.data
        }

        fun update(quoteDetail: QuoteDetail) {
            data.text = quoteDetail.data
        }
    }

    companion object {
        private val DETAIL_COMPARATOR = object : DiffUtil.ItemCallback<QuoteDetail>() {
            override fun areItemsTheSame(oldItem: QuoteDetail, newItem: QuoteDetail): Boolean {
                return oldItem.title == newItem.title
            }

            override fun areContentsTheSame(oldItem: QuoteDetail, newItem: QuoteDetail): Boolean {
                return oldItem.data == newItem.data
            }

            override fun getChangePayload(oldItem: QuoteDetail, newItem: QuoteDetail): Any? {
                return newItem
            }
        }
    }
}

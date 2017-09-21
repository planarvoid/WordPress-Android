package com.soundcloud.android.search.history

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter
import com.soundcloud.android.search.history.SearchHistoryAdapter.ViewHolder
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.autocompletion_item.view.*
import javax.inject.Inject

internal class SearchHistoryAdapter
constructor(searchHistoryCellRenderer: SearchHistoryCellRenderer) : PagingRecyclerItemAdapter<SearchHistoryItem, ViewHolder>(searchHistoryCellRenderer) {

    internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var text: TextView = itemView.search_title
    }

    override fun createViewHolder(itemView: View): ViewHolder = ViewHolder(itemView)

    override fun getBasicItemViewType(position: Int): Int = 0

    internal class Factory
    @Inject
    internal constructor() {
        fun create(searchHistoryCellRendererFactory: SearchHistoryCellRenderer.Factory,
                   itemClickListener: PublishSubject<SearchHistoryItem>,
                   autocompleteArrowClickListener: PublishSubject<SearchHistoryItem>)
                = SearchHistoryAdapter(searchHistoryCellRendererFactory.create(itemClickListener, autocompleteArrowClickListener))
    }

}

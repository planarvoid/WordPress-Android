package com.soundcloud.android.search.main

import android.support.v7.widget.RecyclerView
import android.view.View
import com.soundcloud.android.presentation.CellRendererBinding
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter
import com.soundcloud.android.presentation.RecyclerItemAdapter
import com.soundcloud.android.search.SearchItemRenderer
import com.soundcloud.android.utils.OpenForTesting

@OpenForTesting
internal class SearchAdapter(searchItemRenderer: SearchItemRenderer<SearchItemViewModel> = SearchItemRenderer(),
                             emptySearchRenderer: EmptySearchRenderer = EmptySearchRenderer(),
                             searchListener: SearchItemRenderer.SearchListener) : PagingRecyclerItemAdapter<SearchItemViewModel, RecyclerView.ViewHolder>(
        CellRendererBinding(Kind.SEARCH_ITEM.ordinal,
                            searchItemRenderer),
        CellRendererBinding(Kind.EMPTY_CARD.ordinal,
                            emptySearchRenderer)) {

    init {
        searchItemRenderer.setSearchListener(searchListener)
    }

    override fun getBasicItemViewType(position: Int): Int =
            when (getItem(position)) {
                is SearchItemViewModel.SearchCard -> Kind.SEARCH_ITEM.ordinal
                is SearchItemViewModel.EmptyCard -> Kind.EMPTY_CARD.ordinal
            }

    override fun createViewHolder(view: View?): RecyclerView.ViewHolder = RecyclerItemAdapter.ViewHolder(view)

    enum class Kind {
        SEARCH_ITEM,
        EMPTY_CARD
    }
}

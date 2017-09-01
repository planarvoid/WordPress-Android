package com.soundcloud.android.discovery

import android.support.v7.widget.RecyclerView
import android.view.View
import com.soundcloud.android.presentation.CellRendererBinding
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter
import com.soundcloud.android.presentation.RecyclerItemAdapter
import com.soundcloud.android.search.SearchItemRenderer
import com.soundcloud.android.search.SearchItemRenderer.SearchListener
import com.soundcloud.android.utils.OpenForTesting
import io.reactivex.Observable
import javax.inject.Inject

@OpenForTesting
internal class DiscoveryAdapter(searchItemRenderer: SearchItemRenderer<DiscoveryCardViewModel>,
                                private val singleSelectionContentCardRenderer: SingleSelectionContentCardRenderer,
                                private val multipleContentSelectionCardRenderer: MultipleContentSelectionCardRenderer,
                                emptyCardRenderer: EmptyCardRenderer,
                                searchListener: SearchListener) :
        PagingRecyclerItemAdapter<DiscoveryCardViewModel, RecyclerView.ViewHolder>(CellRendererBinding(DiscoveryCard.Kind.SEARCH_ITEM.ordinal,
                                                                                                       searchItemRenderer),
                                                                                   CellRendererBinding(DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD.ordinal,
                                                                                                       singleSelectionContentCardRenderer),
                                                                                   CellRendererBinding(DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD.ordinal,
                                                                                                       multipleContentSelectionCardRenderer),
                                                                                   CellRendererBinding(DiscoveryCard.Kind.EMPTY_CARD.ordinal,
                                                                                                       emptyCardRenderer)) {

    init {
        searchItemRenderer.setSearchListener(searchListener)
    }

    override fun createViewHolder(view: View) = RecyclerItemAdapter.ViewHolder(view)

    override fun getBasicItemViewType(position: Int) = getItem(position).kind.ordinal

    fun selectionItemClick(): Observable<SelectionItemViewModel> = Observable.merge(singleSelectionContentCardRenderer.selectionItemClick(),
                                                                                    multipleContentSelectionCardRenderer.selectionItemClick())

    @OpenForTesting
    class Factory
    @Inject
    constructor(private val searchItemRenderer: SearchItemRenderer<DiscoveryCardViewModel>,
                private val singleSelectionContentCardRenderer: SingleSelectionContentCardRenderer,
                private val multipleContentSelectionCardRenderer: MultipleContentSelectionCardRenderer,
                private val emptyCardRenderer: EmptyCardRenderer) {
        fun create(searchListener: SearchListener): DiscoveryAdapter =
                DiscoveryAdapter(searchItemRenderer, singleSelectionContentCardRenderer, multipleContentSelectionCardRenderer, emptyCardRenderer, searchListener)
    }
}

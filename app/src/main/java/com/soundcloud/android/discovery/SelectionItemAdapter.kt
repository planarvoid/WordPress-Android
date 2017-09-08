package com.soundcloud.android.discovery

import android.support.v7.widget.RecyclerView
import android.view.View
import com.soundcloud.android.discovery.DiscoveryCardViewModel.MultipleContentSelectionCard
import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.RecyclerItemAdapter
import com.soundcloud.android.utils.OpenForTesting
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

@OpenForTesting
class SelectionItemAdapter(selectionItemRendererFactory: SelectionItemRenderer.Factory,
                           selectionItemClickListener: PublishSubject<SelectionItemViewModel>,
                           var selectionUrn: Urn? = null) :
        RecyclerItemAdapter<SelectionItemViewModel, RecyclerView.ViewHolder>(selectionItemRendererFactory.create(selectionItemClickListener)) {

    fun updateSelection(selection: MultipleContentSelectionCard) {
        selectionUrn = selection.selectionUrn
        clear()
        onNext(selection.selectionItems)
    }

    override fun createViewHolder(itemView: View) = RecyclerItemAdapter.ViewHolder(itemView)

    override fun getBasicItemViewType(position: Int): Int = 0

    @OpenForTesting
    data class Factory
    @Inject
    constructor(private val selectionItemRendererFactory: SelectionItemRenderer.Factory) {
        fun create(selectionItemClickListener: PublishSubject<SelectionItemViewModel>) = SelectionItemAdapter(selectionItemRendererFactory, selectionItemClickListener)
    }
}

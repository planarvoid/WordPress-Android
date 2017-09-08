package com.soundcloud.android.discovery

import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.soundcloud.android.R
import com.soundcloud.android.discovery.DiscoveryCardViewModel.MultipleContentSelectionCard
import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.utils.OpenForTesting
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.discovery_multiple_content_selection_card.view.*
import javax.inject.Inject

@OpenForTesting
class MultipleContentSelectionCardRenderer
@Inject
constructor(private val selectionItemAdapterFactory: SelectionItemAdapter.Factory) : CellRenderer<MultipleContentSelectionCard> {

    private val scrollingState = mutableMapOf<Urn, Parcelable>()
    internal val selectionItemInCardClickListener: PublishSubject<SelectionItemViewModel> = PublishSubject.create()

    internal fun selectionItemClick(): Observable<SelectionItemViewModel> {
        return selectionItemInCardClickListener
    }

    override fun createItemView(viewGroup: ViewGroup): View {
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.discovery_multiple_content_selection_card, viewGroup, false)
        initCarousel(view, view.selection_playlists_carousel)
        return view
    }

    private fun initCarousel(cardView: View, recyclerView: RecyclerView) {
        val context = recyclerView.context
        val adapter = selectionItemAdapterFactory.create(selectionItemInCardClickListener)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter

        cardView.tag = adapter
    }

    override fun bindItemView(position: Int, view: View, list: List<MultipleContentSelectionCard>) {
        val selectionCard = list[position]
        bindTitle(view, selectionCard)
        bindDescription(view, selectionCard)
        bindCarousel(view, selectionCard)
    }

    private fun bindTitle(view: View, selectionCard: MultipleContentSelectionCard) {
        bindText(view.selection_title, selectionCard.title)
    }

    private fun bindDescription(view: View, selectionCard: MultipleContentSelectionCard) {
        bindText(view.selection_description, selectionCard.description)
    }

    private fun bindText(textView: TextView, text: String?) {
        if (text != null) {
            textView.visibility = View.VISIBLE
            textView.text = text
        } else {
            textView.visibility = View.GONE
        }
    }

    private fun bindCarousel(view: View, selectionCard: MultipleContentSelectionCard) {
        val recyclerView = view.selection_playlists_carousel

        (view.tag as? SelectionItemAdapter)?.apply {
            saveOldScrollingState(this, recyclerView)
            this.updateSelection(selectionCard)
            loadScrollingState(this, recyclerView)
        }
    }

    private fun saveOldScrollingState(adapter: SelectionItemAdapter, recyclerView: RecyclerView) {
        adapter.selectionUrn?.let { urn -> scrollingState.put(urn, recyclerView.layoutManager.onSaveInstanceState()) }
    }

    private fun loadScrollingState(adapter: SelectionItemAdapter, recyclerView: RecyclerView) {
        val selectionUrn = adapter.selectionUrn
        if (selectionUrn != null && scrollingState.containsKey(selectionUrn)) {
            recyclerView.layoutManager.onRestoreInstanceState(scrollingState[selectionUrn])
        } else {
            recyclerView.scrollToPosition(0)
        }
    }
}

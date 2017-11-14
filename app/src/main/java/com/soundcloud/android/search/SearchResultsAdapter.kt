package com.soundcloud.android.search

import android.support.v7.widget.RecyclerView
import android.view.View
import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.CellRendererBinding
import com.soundcloud.android.presentation.ListItem
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter
import com.soundcloud.android.presentation.RecyclerItemAdapter
import com.soundcloud.android.search.SearchPremiumContentRenderer.OnPremiumContentClickListener
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_HEADER
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_PLAYLIST
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_PREMIUM_CONTENT
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_TRACK
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_UPSELL
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_USER
import com.soundcloud.android.search.SearchUpsellRenderer.OnUpsellClickListener
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.view.adapters.PlayingTrackAware
import javax.inject.Inject

@OpenForTesting
internal class SearchResultsAdapter
@Inject
constructor(trackItemRenderer: SearchTrackItemRenderer,
            playlistItemRenderer: SearchPlaylistItemRenderer,
            userItemRenderer: SearchUserItemRenderer,
            private val searchPremiumContentRenderer: SearchPremiumContentRenderer,
            private val searchUpsellRenderer: SearchUpsellRenderer,
            searchResultHeaderRenderer: SearchResultHeaderRenderer) :
        PagingRecyclerItemAdapter<ListItem, RecyclerView.ViewHolder>(CellRendererBinding(TYPE_TRACK.ordinal, trackItemRenderer),
                                                                     CellRendererBinding(TYPE_PLAYLIST.ordinal, playlistItemRenderer),
                                                                     CellRendererBinding(TYPE_USER.ordinal, userItemRenderer),
                                                                     CellRendererBinding(TYPE_PREMIUM_CONTENT.ordinal, searchPremiumContentRenderer),
                                                                     CellRendererBinding(TYPE_UPSELL.ordinal, searchUpsellRenderer),
                                                                     CellRendererBinding(TYPE_HEADER.ordinal, searchResultHeaderRenderer)), PlayingTrackAware {

    fun getResultItems(): List<ListItem> {
        val item = getItem(0)
        return if (item is UpsellSearchableItem || item is SearchPremiumItem) {
            getItems().subList(1, getItems().size)
        } else getItems()
    }

    internal enum class Kind {
        TYPE_USER,
        TYPE_TRACK,
        TYPE_PLAYLIST,
        TYPE_PREMIUM_CONTENT,
        TYPE_UPSELL,
        TYPE_HEADER
    }

    override fun getBasicItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchResultHeaderRenderer.SearchResultHeader -> TYPE_HEADER.ordinal
            is UpsellSearchableItem -> TYPE_UPSELL.ordinal
            is SearchPremiumItem -> TYPE_PREMIUM_CONTENT.ordinal
            is SearchTrackItem -> TYPE_TRACK.ordinal
            is SearchPlaylistItem -> TYPE_PLAYLIST.ordinal
            is SearchUserItem -> TYPE_USER.ordinal
            else -> {
                throw IllegalStateException("Unexpected item type in " + SearchResultsAdapter::class.java.simpleName + " - " + getItem(position).toString())
            }
        }
    }

    override fun updateNowPlaying(currentlyPlayingUrn: Urn) {
        val newItems = items.map { viewModel ->
            when (viewModel) {
                is SearchTrackItem -> {
                    val isPlaying = viewModel.urn == currentlyPlayingUrn
                    val trackItem = viewModel.trackItem.withPlayingState(isPlaying)
                    viewModel.copy(trackItem = trackItem)
                }
                is SearchPremiumItem -> {
                    viewModel.setTrackIsPlaying(currentlyPlayingUrn)
                    viewModel
                }
                else -> viewModel
            }
        }
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun createViewHolder(itemView: View): RecyclerItemAdapter.ViewHolder {
        return RecyclerItemAdapter.ViewHolder(itemView)
    }

    fun setPremiumContentListener(listener: OnPremiumContentClickListener) {
        this.searchPremiumContentRenderer.setPremiumContentListener(listener)
    }

    fun setUpsellListener(listener: OnUpsellClickListener) {
        this.searchUpsellRenderer.setUpsellClickListener(listener)
    }
}

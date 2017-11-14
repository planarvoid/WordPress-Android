package com.soundcloud.android.search

import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.tracks.TrackItemRenderer
import com.soundcloud.android.utils.OpenForTesting
import javax.inject.Inject

@OpenForTesting
class SearchTrackItemRenderer
@Inject
constructor(private val trackItemRenderer: TrackItemRenderer) :
        CellRenderer<SearchTrackItem> {

    override fun createItemView(parent: ViewGroup): View = trackItemRenderer.createItemView(parent)

    override fun bindItemView(position: Int, itemView: View, items: MutableList<SearchTrackItem>) {
        items.getOrNull(position)?.let {
            trackItemRenderer.bindSearchTrackView(it.trackItem, itemView, position, it.queryUrn)
        }
    }
}

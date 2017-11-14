package com.soundcloud.android.search

import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.view.adapters.PlaylistItemRenderer
import javax.inject.Inject

@OpenForTesting
class SearchPlaylistItemRenderer
@Inject
constructor(private val playlistItemRenderer: PlaylistItemRenderer): CellRenderer<SearchPlaylistItem> {
    override fun createItemView(parent: ViewGroup): View = playlistItemRenderer.createItemView(parent)

    override fun bindItemView(position: Int, itemView: View, items: List<SearchPlaylistItem>) {
        items.getOrNull(position)?.let {
            playlistItemRenderer.bindSearchPlaylistView(it.playlistItem, itemView, it.queryUrn)
        }
    }
}

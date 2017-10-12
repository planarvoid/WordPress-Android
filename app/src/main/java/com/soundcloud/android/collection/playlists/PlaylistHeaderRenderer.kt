package com.soundcloud.android.collection.playlists

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.utils.ViewUtils
import kotlinx.android.synthetic.main.collection_playlist_header.view.*
import javax.inject.Inject

class PlaylistHeaderRenderer
@Inject
internal constructor(private val resources: Resources) : CellRenderer<PlaylistCollectionHeaderItem> {

    private var onSettingsClickListener: OnSettingsClickListener? = null

    interface OnSettingsClickListener {
        fun onSettingsClicked(view: View)
    }

    override fun createItemView(parent: ViewGroup): View {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.collection_playlist_header, parent, false)
        view.btn_collections_playlist_options.setOnClickListener { onSettingsClickListener?.onSettingsClicked(it) }
        ViewUtils.extendTouchArea(view.btn_collections_playlist_options)
        return view
    }

    override fun bindItemView(position: Int, view: View, list: List<PlaylistCollectionHeaderItem>) = with(list[position]) {
        view.header_text.text = resources.getQuantityString(kind().headerResource(), playlistCount, playlistCount)
    }

    fun setOnSettingsClickListener(onSettingsClickListener: OnSettingsClickListener) {
        this.onSettingsClickListener = onSettingsClickListener
    }
}

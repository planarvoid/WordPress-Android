package com.soundcloud.android.olddiscovery.recommendedplaylists

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.image.ImageOperations
import com.soundcloud.android.image.ImageStyle
import com.soundcloud.android.playlists.PlaylistItem
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.java.optional.Optional
import kotlinx.android.synthetic.main.carousel_playlist_item.view.*
import kotlinx.android.synthetic.main.carousel_playlist_item_fixed_width.view.*
import javax.inject.Inject

class CarouselPlaylistItemRenderer
@Inject
constructor(private val imageOperations: ImageOperations) : CellRenderer<PlaylistItem> {
    private var playlistListener: PlaylistListener? = null

    override fun createItemView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context)
                .inflate(R.layout.carousel_playlist_item_fixed_width, parent, false)
    }

    override fun bindItemView(position: Int, view: View, list: List<PlaylistItem>) {
        val playlist = list[position]

        with(view) {
            artwork.showWithoutPlaceholder(playlist.imageUrlTemplate, Optional.of(ImageStyle.SQUARE), playlist.urn, imageOperations)

            title.text = playlist.title()
            track_count.text = playlist.trackCount().toString()
            secondary_text.text = playlist.creatorName()

            private_indicator.visibility = if (playlist.isPrivate) View.VISIBLE else View.GONE
            like_indicator.visibility = if (playlist.isUserLike) View.VISIBLE else View.GONE
            overflow_button.visibility = View.GONE

            setOnClickListener { view -> playlistListener!!.onPlaylistClick(view.context, playlist, position) }
        }
    }

    fun setPlaylistListener(playlistListener: PlaylistListener) {
        this.playlistListener = playlistListener
    }

    interface PlaylistListener {
        fun onPlaylistClick(context: Context, playlist: PlaylistItem, position: Int)
    }
}

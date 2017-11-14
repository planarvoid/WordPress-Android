package com.soundcloud.android.search

import com.soundcloud.android.model.Urn
import com.soundcloud.android.playlists.PlaylistItem
import com.soundcloud.android.presentation.ListItem
import com.soundcloud.java.optional.Optional

data class SearchPlaylistItem(val playlistItem: PlaylistItem,
                              val queryUrn: Optional<Urn>,
                              override val urn: Urn = playlistItem.urn,
                              override val imageUrlTemplate: Optional<String> = playlistItem.imageUrlTemplate) : ListItem

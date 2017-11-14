package com.soundcloud.android.search

import com.soundcloud.android.playlists.PlaylistItem
import com.soundcloud.android.presentation.ListItem
import com.soundcloud.android.tracks.TrackItem
import com.soundcloud.android.users.UserItem

fun convertSearchResultToSearchItems(searchResult: SearchResult): List<ListItem> {
    return searchResult.items.map {
        when (it) {
            is TrackItem -> SearchTrackItem(it, searchResult.queryUrn)
            is PlaylistItem -> SearchPlaylistItem(it, searchResult.queryUrn)
            is UserItem -> SearchUserItem(it, searchResult.queryUrn)
            else -> it
        }
    }
}

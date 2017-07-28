package com.soundcloud.android.presentation;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import io.reactivex.Observable;

import java.util.List;
import java.util.Map;

public interface EntityItemEmitter {
    Observable<List<TrackItem>> trackItems(List<ApiTrack> apiTracks);

    Observable<Map<Urn, TrackItem>> trackItemsAsMap(List<ApiTrack> apiTracks);

    Observable<List<PlaylistItem>> playlistItems(List<ApiPlaylist> apiPlaylists);

    Observable<Map<Urn, PlaylistItem>> playlistItemsAsMap(List<ApiPlaylist> apiPlaylists);

    Observable<List<UserItem>> userItems(List<ApiUser> apiUsers);

    Observable<Map<Urn, UserItem>> userItemsAsMap(List<ApiUser> apiUsers);
}

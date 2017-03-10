package com.soundcloud.android.presentation;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.profile.ApiPlayableSource;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserItem;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class EntityItemCreator {

    @Inject
    public EntityItemCreator() {
    }

    public PlayableItem playableItem(ApiPlayableSource apiPlayableSource) {
        if (apiPlayableSource.getTrack().isPresent()) {
            return trackItem(Track.from(apiPlayableSource.getTrack().get()));
        } else if (apiPlayableSource.getPlaylist().isPresent()) {
            return playlistItem(Playlist.from(apiPlayableSource.getPlaylist().get()));
        } else {
            throw new IllegalArgumentException("Not a playable source " + apiPlayableSource);
        }
    }

    public TrackItem trackItem(Track track) {
        return TrackItem.from(track);
    }

    public TrackItem trackItem(Track track, StreamEntity streamEntity) {
        return TrackItem.from(track, streamEntity);
    }

    public TrackItem trackItem(ApiTrack apiTrack) {
        return TrackItem.from(apiTrack);
    }

    public TrackItem trackItem(ApiTrackPost apiTrackPost) {
        return trackItem(apiTrackPost.getApiTrack());
    }

    public PlaylistItem playlistItem(Playlist playlist) {
        return PlaylistItem.from(playlist);
    }

    public PlaylistItem playlistItem(Playlist playlist, StreamEntity streamEntity) {
        return PlaylistItem.from(playlist, streamEntity);
    }

    public PlaylistItem playlistItem(ApiPlaylist apiPlaylist) {
        return playlistItem(Playlist.from(apiPlaylist));
    }

    public PlaylistItem playlistItem(ApiPlaylistPost apiPlaylistPost) {
        return playlistItem(apiPlaylistPost.getApiPlaylist());
    }

    public UserItem userItem(User user) {
        return UserItem.from(user);
    }

    public UserItem userItem(ApiUser apiUser) {
        return userItem(User.fromApiUser(apiUser));
    }

    public Map<Urn, TrackItem> convertTrackMap(Map<Urn, Track> map) {
        Map<Urn, TrackItem> trackItemMap = new HashMap<>();
        for(Map.Entry<Urn, Track> entry : map.entrySet()) {
            trackItemMap.put(entry.getKey(), trackItem(entry.getValue()));
        }
        return trackItemMap;
    }

}

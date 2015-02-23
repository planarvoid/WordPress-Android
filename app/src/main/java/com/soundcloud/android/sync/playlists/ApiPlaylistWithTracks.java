package com.soundcloud.android.sync.playlists;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;

public class ApiPlaylistWithTracks {

    private final ApiPlaylist playlist;
    private final ModelCollection<ApiTrack> playlistTracks;

    public ApiPlaylistWithTracks(@JsonProperty("playlist") ApiPlaylist playlist,
                                 @JsonProperty("tracks") ModelCollection<ApiTrack> playlistTracks) {
        this.playlist = playlist;
        this.playlistTracks = playlistTracks;
    }

    public ModelCollection<ApiTrack> getPlaylistTracks() {
        return playlistTracks;
    }

    public ApiPlaylist getPlaylist() {
        return playlist;
    }
}

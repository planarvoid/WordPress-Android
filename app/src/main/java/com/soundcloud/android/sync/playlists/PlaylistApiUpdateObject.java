package com.soundcloud.android.sync.playlists;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

import java.util.List;

@AutoValue
public abstract class PlaylistApiUpdateObject {

    @Nullable
    public abstract String getTitle();

    @Nullable
    public abstract Boolean getPublic();

    @JsonProperty("track_urns")
    public abstract List<String> getTrackUrns();

    public static PlaylistApiUpdateObject create(Optional<LocalPlaylistChange> playlist, List<Urn> newTrackList) {
        final String title = playlist.transform(LocalPlaylistChange::title).orNull();
        return new AutoValue_PlaylistApiUpdateObject(title, isPublic(playlist), Urns.toString(newTrackList));
    }

    @Nullable
    private static Boolean isPublic(Optional<LocalPlaylistChange> playlist) {
        return playlist.isPresent() ? !playlist.get().isPrivate() : null;
    }
}

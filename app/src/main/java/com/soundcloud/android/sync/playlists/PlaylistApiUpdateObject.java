package com.soundcloud.android.sync.playlists;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.PropertySet;

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

    public static PlaylistApiUpdateObject create(PropertySet playlist, List<Urn> newTrackList) {
        final String title = playlist.getOrElseNull(PlaylistProperty.TITLE);
        return new AutoValue_PlaylistApiUpdateObject(title, isPublic(playlist), Urns.toString(newTrackList));
    }

    @Nullable
    private static Boolean isPublic(PropertySet playlist) {
        return playlist.contains(PlaylistProperty.IS_PRIVATE) ? !playlist.get(PlaylistProperty.IS_PRIVATE) : null;
    }
}

package com.soundcloud.android.sync.playlists;


import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.legacy.Params;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.java.collections.PropertySet;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class PlaylistApiUpdateObject {

    @Nullable
    public abstract String getTitle();

    @Nullable
    public abstract String getSharing();

    public abstract List<Map<String, Long>> getTracks();

    public static PlaylistApiUpdateObject create(PropertySet playlist, List<Urn> newTrackList) {
        final String title = playlist.getOrElseNull(PlaylistProperty.TITLE);
        final String sharing = playlist.contains(PlaylistProperty.IS_PRIVATE) ? getSharing(playlist.get(PlaylistProperty.IS_PRIVATE)) : null;
        final List<Map<String, Long>> trackList = getTrackList(newTrackList);
        return new AutoValue_PlaylistApiUpdateObject(title, sharing, trackList);
    }

    private static String getSharing(Boolean isPrivate) {
        return isPrivate ? Params.Track.PRIVATE : Params.Track.PUBLIC;
    }

    @NonNull
    private static List<Map<String, Long>> getTrackList(List<Urn> newTrackList) {
        List<Map<String, Long>> tracks = new ArrayList<>(newTrackList.size());
        for (Urn track : newTrackList) {
            tracks.add(Collections.singletonMap("id", track.getNumericId()));
        }
        return tracks;
    }
}

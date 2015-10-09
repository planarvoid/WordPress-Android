package com.soundcloud.android.offline;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.List;

@AutoValue
public abstract class OfflineTrackContext {

    public static OfflineTrackContext create(Urn track, Urn creator, List<Urn> inPlaylists, boolean inLikes) {
        return new AutoValue_OfflineTrackContext(track, creator, inPlaylists, inLikes);
    }

    public abstract Urn getTrack();

    public abstract Urn getCreator();

    public abstract List<Urn> getPlaylists();

    public abstract boolean isLiked();
}

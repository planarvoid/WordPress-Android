package com.soundcloud.android.sync.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class PlaylistTrackChange {
    public abstract Urn urn();
    public abstract boolean added();
    public abstract boolean removed();

    public static PlaylistTrackChange createEmpty(Urn urn) {
        return new AutoValue_PlaylistTrackChange(urn, false, false);
    }

    public static PlaylistTrackChange createAdded(Urn urn) {
        return new AutoValue_PlaylistTrackChange(urn, true, false);
    }

    public static PlaylistTrackChange createRemoved(Urn urn) {
        return new AutoValue_PlaylistTrackChange(urn, false, true);
    }
}

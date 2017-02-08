package com.soundcloud.android.sync.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class LocalPlaylistChange {
    public abstract Urn urn();

    public abstract String title();

    public abstract boolean isPrivate();

    public static LocalPlaylistChange create(Urn urn, String titile, boolean isPrivate) {
        return new AutoValue_LocalPlaylistChange(urn, titile, isPrivate);
    }
}

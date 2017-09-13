package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class PlaylistAssociation {

    public static PlaylistAssociation create(Playlist playlist, Association association) {
        return new AutoValue_PlaylistAssociation(playlist, association);
    }

    public abstract Playlist getPlaylist();

    public abstract Association getAssociation();

    public Urn getTargetUrn() {
        return getPlaylist().urn();
    }

}

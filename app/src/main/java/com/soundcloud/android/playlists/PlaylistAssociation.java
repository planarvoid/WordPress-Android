package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;

import java.util.Date;

@AutoValue
public abstract class PlaylistAssociation {

    public static PlaylistAssociation create(Playlist playlist, Date createdAt) {
        return new AutoValue_PlaylistAssociation(playlist, createdAt);
    }

    public abstract Playlist getPlaylist();

    public abstract Date getCreatedAt();

}

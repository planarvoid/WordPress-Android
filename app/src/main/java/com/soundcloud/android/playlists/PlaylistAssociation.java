package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.functions.Function;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

@AutoValue
public abstract class PlaylistAssociation {

    public static PlaylistAssociation create(PlaylistItem playlistItem, Date createdAt) {
        return new AutoValue_PlaylistAssociation(playlistItem, createdAt);
    }

    public abstract PlaylistItem getPlaylistItem();

    public abstract Date getCreatedAt();

    public static Function<PlaylistAssociation, PlaylistItem> GET_PLAYLIST_ITEM = input -> input.getPlaylistItem();
}

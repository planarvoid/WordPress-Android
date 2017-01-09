package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import rx.functions.Func1;

import java.util.Map;
import java.util.Set;

public abstract class PlaylistChangedEvent<T> {
    public static final Func1<PlaylistChangedEvent, Set<Urn>> TO_URNS = event -> event.changeMap().keySet();

    public enum Kind {
        TRACK_ADDED, TRACK_REMOVED, PLAYLIST_MARKED_FOR_DOWNLOAD
    }

    public abstract Kind kind();

    public abstract Map<Urn, T> changeMap();

    public abstract UpdatablePlaylistItem apply(UpdatablePlaylistItem updatablePlaylistItem);
}

package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;

import java.util.Collections;
import java.util.Map;

@AutoValue
public abstract class PlaylistTrackCountChangedEvent extends PlaylistChangedEvent<Integer> {
    public static PlaylistTrackCountChangedEvent fromTrackAddedToPlaylist(Map<Urn, Integer> trackCounts) {
        return new AutoValue_PlaylistTrackCountChangedEvent(Kind.TRACK_ADDED, trackCounts);
    }

    public static PlaylistTrackCountChangedEvent fromTrackAddedToPlaylist(Urn urn, Integer trackCount) {
        return new AutoValue_PlaylistTrackCountChangedEvent(Kind.TRACK_ADDED, Collections.singletonMap(urn, trackCount));
    }

    public static PlaylistTrackCountChangedEvent fromTrackRemovedFromPlaylist(Urn urn, Integer trackCount) {
        return new AutoValue_PlaylistTrackCountChangedEvent(Kind.TRACK_REMOVED, Collections.singletonMap(urn, trackCount));
    }

    @Override
    public UpdatablePlaylistItem apply(UpdatablePlaylistItem updatablePlaylistItem) {
        if (changeMap().containsKey(updatablePlaylistItem.getUrn())) {
            return updatablePlaylistItem.updatedWithTrackCount(changeMap().get(updatablePlaylistItem.getUrn()));
        }
        return updatablePlaylistItem;
    }
}

package com.soundcloud.android.events;

import static com.soundcloud.android.events.PlaylistChangedEvent.Kind.PLAYLIST_MARKED_FOR_DOWNLOAD;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class PlaylistMarkedForOfflineStateChangedEvent extends PlaylistChangedEvent<Boolean> {
    public static PlaylistMarkedForOfflineStateChangedEvent fromPlaylistsMarkedForDownload(List<Urn> playlistUrns) {
        return new AutoValue_PlaylistMarkedForOfflineStateChangedEvent(PLAYLIST_MARKED_FOR_DOWNLOAD, toMarkedForOfflineMap(playlistUrns, true));
    }

    public static PlaylistMarkedForOfflineStateChangedEvent fromPlaylistsUnmarkedForDownload(List<Urn> playlistUrns) {
        return new AutoValue_PlaylistMarkedForOfflineStateChangedEvent(PLAYLIST_MARKED_FOR_DOWNLOAD, toMarkedForOfflineMap(playlistUrns, false));
    }

    private static Map<Urn, Boolean> toMarkedForOfflineMap(List<Urn> playlistUrns, boolean markedForOffline) {
        final Map<Urn, Boolean> changeSet = new HashMap<>(playlistUrns.size());
        for (Urn playlistUrn : playlistUrns) {
            changeSet.put(playlistUrn, markedForOffline);
        }
        return changeSet;
    }

    @Override
    public UpdatablePlaylistItem apply(UpdatablePlaylistItem updatablePlaylistItem) {
        if (changeMap().containsKey(updatablePlaylistItem.getUrn())) {
            return updatablePlaylistItem.updatedWithMarkedForOffline(changeMap().get(updatablePlaylistItem.getUrn()));
        }
        return updatablePlaylistItem;
    }
}

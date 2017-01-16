package com.soundcloud.android.events;

import static com.soundcloud.android.events.PlaylistChangedEvent.Kind.PLAYLIST_EDITED;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class PlaylistEntityChangedEvent extends PlaylistChangedEvent<Playlist> {
    public static PlaylistEntityChangedEvent fromPlaylistPushedToServer(Urn localUrn, Playlist playlist) {
        final Map<Urn, Playlist> changeMap = Collections.singletonMap(localUrn, playlist);
        return new AutoValue_PlaylistEntityChangedEvent(Kind.PLAYLIST_PUSHED_TO_SERVER, changeMap);
    }

    public static PlaylistEntityChangedEvent fromPlaylistEdited(Playlist playlist) {
        final Map<Urn, Playlist> changeMap = Collections.singletonMap(playlist.urn(), playlist);
        return new AutoValue_PlaylistEntityChangedEvent(PLAYLIST_EDITED, changeMap);
    }


    public static PlaylistEntityChangedEvent forUpdate(Collection<Playlist> input) {
        final Map<Urn, Playlist> changeMap = new HashMap<>(input.size());
        for (Playlist playlist : input) {
            changeMap.put(playlist.urn(), playlist);
        }
        return new AutoValue_PlaylistEntityChangedEvent(Kind.PLAYLIST_UPDATED, changeMap);
    }

    @Override
    public UpdatablePlaylistItem apply(UpdatablePlaylistItem updatablePlaylistItem) {
        if (changeMap().containsKey(updatablePlaylistItem.getUrn())) {
            final Playlist playlist = changeMap().get(updatablePlaylistItem.getUrn());
            return updatablePlaylistItem.updatedWithPlaylist(playlist);
        }
        return updatablePlaylistItem;
    }
}

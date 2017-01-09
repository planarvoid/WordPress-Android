package com.soundcloud.android.events;

import static com.soundcloud.android.events.PlaylistChangedEvent.Kind.PLAYLIST_EDITED;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class PlaylistEntityChangedEvent extends PlaylistChangedEvent<PlaylistItem> {
    public static PlaylistEntityChangedEvent fromPlaylistPushedToServer(Urn localUrn, PlaylistItem playlistItem) {
        final Map<Urn, PlaylistItem> changeMap = Collections.singletonMap(localUrn, playlistItem);
        return new AutoValue_PlaylistEntityChangedEvent(Kind.PLAYLIST_PUSHED_TO_SERVER, changeMap);
    }

    public static PlaylistEntityChangedEvent fromPlaylistEdited(PlaylistItem playlistItem) {
        final Map<Urn, PlaylistItem> changeMap = Collections.singletonMap(playlistItem.getUrn(), playlistItem);
        return new AutoValue_PlaylistEntityChangedEvent(PLAYLIST_EDITED, changeMap);
    }

    public static PlaylistEntityChangedEvent forUpdate(Collection<PlaylistItem> input) {
        final Map<Urn, PlaylistItem> changeMap = new HashMap<>(input.size());
        for (PlaylistItem playlistItem : input) {
            changeMap.put(playlistItem.getUrn(), playlistItem);
        }
        return new AutoValue_PlaylistEntityChangedEvent(Kind.PLAYLIST_UPDATED, changeMap);
    }

    @Override
    public UpdatablePlaylistItem apply(UpdatablePlaylistItem updatablePlaylistItem) {
        if (changeMap().containsKey(updatablePlaylistItem.getUrn())) {
            return updatablePlaylistItem.updatedWithPlaylistItem(changeMap().get(updatablePlaylistItem.getUrn()));
        }
        return updatablePlaylistItem;
    }
}

package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.sync.entities.PublishUpdateEvent;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import java.util.Collection;

public class PublishPlaylistUpdateEvent extends PublishUpdateEvent<ApiPlaylist> {
    private final EventBus eventBus;

    @Inject
    public PublishPlaylistUpdateEvent(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Boolean call(Collection<ApiPlaylist> input) {
        if (input.size() > 0) {
            final Collection<PlaylistItem> playlistItems = MoreCollections.transform(input, PlaylistItem::from);
            eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.forUpdate(playlistItems));
            return true;
        }
        return false;
    }
}

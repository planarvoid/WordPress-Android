package com.soundcloud.android.sync.commands;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import java.util.Collection;

public class PublishPlaylistUpdateEventCommand extends PublishUpdateEventCommand<ApiPlaylist> {
    private final EventBus eventBus;

    @Inject
    public PublishPlaylistUpdateEventCommand(EventBus eventBus) {
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
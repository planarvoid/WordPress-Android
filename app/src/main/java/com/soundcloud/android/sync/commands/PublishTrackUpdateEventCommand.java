package com.soundcloud.android.sync.commands;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackChangedEvent;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import java.util.Collection;

public class PublishTrackUpdateEventCommand extends PublishUpdateEventCommand<ApiTrack> {
    private final EventBus eventBus;

    @Inject
    public PublishTrackUpdateEventCommand(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Boolean call(Collection<ApiTrack> input) {
        if (input.size() > 0) {
            final Collection<TrackItem> trackItems = MoreCollections.transform(input, TrackItem::from);
            eventBus.publish(EventQueue.TRACK_CHANGED, TrackChangedEvent.forUpdate(trackItems));
            return true;
        }
        return false;
    }
}

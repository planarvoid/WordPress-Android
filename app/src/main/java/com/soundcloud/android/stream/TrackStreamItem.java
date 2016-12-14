package com.soundcloud.android.stream;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import com.soundcloud.java.collections.PropertySet;

import java.util.Date;

@AutoValue
abstract class TrackStreamItem extends StreamItem implements PlayableViewItem {
    public abstract TrackItem trackItem();

    public abstract boolean promoted();

    public abstract Date createdAt();

    static TrackStreamItem create(TrackItem trackItem, Date createdAt) {
        return new AutoValue_TrackStreamItem(Kind.TRACK, trackItem, false, createdAt);
    }

    static TrackStreamItem createForPromoted(PromotedTrackItem trackItem, Date createdAt) {
        return new AutoValue_TrackStreamItem(Kind.TRACK, trackItem, true, createdAt);
    }

    TrackStreamItem copyWith(PropertySet changeSet) {
        final TrackItem updatedTrackItem = trackItem().updated(changeSet);
        return new AutoValue_TrackStreamItem(Kind.TRACK, updatedTrackItem, this.promoted(), this.createdAt());
    }

    @Override
    public boolean updateNowPlaying(CurrentPlayQueueItemEvent event) {
        return trackItem().updateNowPlaying(event.getCurrentPlayQueueItem().getUrnOrNotSet());
    }
}

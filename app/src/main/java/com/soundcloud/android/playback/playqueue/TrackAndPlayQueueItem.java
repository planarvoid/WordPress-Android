package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.objects.MoreObjects;

public class TrackAndPlayQueueItem {
    public final TrackItem trackItem;
    public final TrackQueueItem playQueueItem;

    TrackAndPlayQueueItem(TrackItem trackItem, TrackQueueItem playQueueItem) {
        this.trackItem = trackItem;
        this.playQueueItem = playQueueItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TrackAndPlayQueueItem that = (TrackAndPlayQueueItem) o;
        return MoreObjects.equal(trackItem, that.trackItem) &&
                MoreObjects.equal(playQueueItem, that.playQueueItem);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(trackItem, playQueueItem);
    }
}

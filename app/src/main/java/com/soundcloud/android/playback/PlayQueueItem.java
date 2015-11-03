package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

public abstract class PlayQueueItem {
    enum Kind {EMPTY, TRACK, VIDEO}

    private PropertySet metaData;

    public boolean isTrack() {
        return this.getKind() == Kind.TRACK;
    }

    public boolean isEmpty() {
        return this.getKind() == Kind.EMPTY;
    }

    public Urn getUrn() {
        checkArgument(this.isTrack(), "Getting URN from non-track play queue item");
        return ((TrackQueueItem) this).getTrackUrn();
    }

    public Urn getUrnOrNotSet() {
        return this.isTrack() ? getUrn() : Urn.NOT_SET;
    }

    public PropertySet getMetaData() {
        return metaData;
    }

    public void setMetaData(PropertySet metaData) {
        this.metaData = metaData;
    }

    public abstract boolean shouldPersist();

    public abstract Kind getKind();

    public static class Empty extends PlayQueueItem {
        @Override
        public boolean shouldPersist() {
            return false;
        }

        @Override
        public Kind getKind() {
            return Kind.EMPTY;
        }
    }
}

package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

public abstract class PlayQueueItem {

    public static final PlayQueueItem EMPTY = new Empty();

    enum Kind {EMPTY, TRACK, VIDEO}

    private PropertySet metaData;

    public boolean isTrack() {
        return this.getKind() == Kind.TRACK;
    }

    public boolean isVideo() {
        return this.getKind() == Kind.VIDEO;
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

    private static class Empty extends PlayQueueItem {
        public Empty() {
            super.setMetaData(PropertySet.create());
        }

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

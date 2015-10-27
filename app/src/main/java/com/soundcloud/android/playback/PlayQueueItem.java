package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

public abstract class PlayQueueItem {
    enum Kind {TRACK, VIDEO}

    private PropertySet metaData;

    public boolean isTrack() {
        return this.getKind() == Kind.TRACK;
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
    
}

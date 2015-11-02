package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

public abstract class PlayQueueItem {

    public static final PlayQueueItem EMPTY = new Empty();

    enum Kind {EMPTY, TRACK, VIDEO}

    private Optional<AdData> adData;

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

    public Optional<AdData> getAdData() {
        return adData;
    }

    public void setAdData(Optional<AdData> adData) {
        this.adData = adData;
    }

    public abstract boolean shouldPersist();

    public abstract Kind getKind();

    private static class Empty extends PlayQueueItem {
        public Empty() {
            super.setAdData(Optional.<AdData>absent());
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

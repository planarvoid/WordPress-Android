package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

public abstract class PlayQueueItem {

    public static final Function<PlayQueueItem, Long> TO_ID = new Function<PlayQueueItem, Long>() {
        public Long apply(PlayQueueItem playQueueItem) {
            return playQueueItem.getUrnOrNotSet().getNumericId();
        }
    };

    public static final PlayQueueItem EMPTY = new Empty();

    enum Kind {EMPTY, TRACK, PLAYLIST, VIDEO}

    private Optional<AdData> adData;

    public boolean isTrack() {
        return this.getKind() == Kind.TRACK;
    }

    public boolean isPlaylist() {
        return this.getKind() == Kind.PLAYLIST;
    }

    public boolean isVideo() {
        return this.getKind() == Kind.VIDEO;
    }

    public boolean isEmpty() {
        return this.getKind() == Kind.EMPTY;
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

    public abstract Urn getUrn();

    public abstract boolean shouldPersist();

    public abstract Kind getKind();

    private static class Empty extends PlayQueueItem {
        public Empty() {
            super.setAdData(Optional.<AdData>absent());
        }

        @Override
        public Urn getUrn() {
            throw new IllegalArgumentException("Attempting to access URN of Empty PlayQueueItem");
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

    @Override
    public final boolean equals(Object o) {
        return (this == o);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}

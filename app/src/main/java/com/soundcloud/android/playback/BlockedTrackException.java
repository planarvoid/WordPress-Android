package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;

public final class BlockedTrackException extends Throwable {
    private final Urn trackUrn;

    public BlockedTrackException(Urn trackUrn) {
        this.trackUrn = trackUrn;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    @Override
    public String toString() {
        return "BlockedTrackException{" +
                "trackUrn=" + trackUrn +
                '}';
    }
}

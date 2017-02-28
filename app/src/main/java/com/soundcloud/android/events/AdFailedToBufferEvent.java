package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class AdFailedToBufferEvent extends TrackingEvent {

    public abstract Urn adUrn();
    public abstract long playbackPosition();
    public abstract int waitPeriod();

    public static AdFailedToBufferEvent create(Urn track, PlaybackProgress position, int failedAdWaitSecs) {
        return new AutoValue_AdFailedToBufferEvent(defaultId(), defaultTimestamp(), Optional.absent(), track, position.getPosition(), failedAdWaitSecs);
    }

    @Override
    public AdFailedToBufferEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_AdFailedToBufferEvent(id(), timestamp(), Optional.of(referringEvent), adUrn(), playbackPosition(), waitPeriod());
    }
}

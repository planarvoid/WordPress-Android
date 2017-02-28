package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackSessionEventArgs;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;

import java.util.UUID;

public class TestEvents {

    public static PlaybackSessionEvent playbackSessionPlayEvent() {
        return playbackSessionPlayEventWithProgress(456L);
    }

    public static PlaybackSessionEvent playbackSessionPlayEventWithProgress(long playbackProgress) {
        return playbackSessionPlayEventWithProgress(playbackProgress, Urn.forTrack(1L));
    }

    public static PlaybackSessionEvent playbackSessionPlayEventWithProgress(long playbackProgress, Urn trackUrn) {
        return PlaybackSessionEvent.forPlay(
                PlaybackSessionEventArgs.create(
                        PlayableFixtures.expectedTrackForAnalytics(trackUrn, Urn.forUser(2L)),
                        new TrackSourceInfo("screen", false), playbackProgress, "hls", "playa", false, false, "uuid", "play-id"));
    }

    public static TrackingEvent unspecifiedTrackingEvent() {
        return new TrackingEvent() {
            public String id() {
                return UUID.randomUUID().toString();
            }

            public long timestamp() {
                return 123L;
            }

            public Optional<ReferringEvent> referringEvent() {
                return Optional.absent();
            }

            public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
                return this;
            }
        };
    }
}

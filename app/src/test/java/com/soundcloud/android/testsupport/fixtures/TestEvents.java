package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.events.LegacyTrackingEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackSessionEventArgs;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;

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
                        TestPropertySets.expectedTrackForAnalytics(trackUrn, Urn.forUser(2L)),
                        new TrackSourceInfo("screen", false), playbackProgress, "hls", "playa", false, false, "uuid", "play-id"));
    }

    public static PlaybackSessionEvent playbackSessionTrackFinishedEvent() {
        return playbackSessionStopEventWithReason(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED, Urn.forTrack(1L));
    }

    private static PlaybackSessionEvent playbackSessionStopEventWithReason(int stopReason, Urn trackUrn) {
        PlaybackSessionEvent previousPlayEvent = playbackSessionPlayEvent();
        final PlaybackSessionEventArgs args = PlaybackSessionEventArgs.create(
                TestPropertySets.expectedTrackForAnalytics(trackUrn, Urn.forUser(2L)),
                new TrackSourceInfo("screen", false), 0L, "hls", "playa", false, false, "uuid", "play-id");

        return PlaybackSessionEvent.forStop(previousPlayEvent, stopReason, args);
    }

    public static TrackingEvent unspecifiedTrackingEvent() {
        return new LegacyTrackingEvent("test", 123L, UUID.randomUUID().toString()) {
        };
    }
}

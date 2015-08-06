package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.tobedevoured.modelcitizen.CreateModelException;

public class TestEvents {

    public static PlaybackSessionEvent playbackSessionPlayEvent() throws CreateModelException {
        return ModelFixtures.create(PlaybackSessionEvent.class);
    }

    public static PlaybackSessionEvent playbackSessionPlayEventWithProgress(long playbackProgress) {
        return PlaybackSessionEvent.forPlay(
                TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(1L), Urn.forUser(2L)),
                Urn.forUser(1), new TrackSourceInfo("screen", false), playbackProgress, 1000L, "hls", "playa", "3g", false);
    }

    public static PlaybackSessionEvent playbackSessionStopEvent() throws CreateModelException {
        return playbackSessionStopEventWithReason(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }

    public static PlaybackSessionEvent playbackSessionTrackFinishedEvent() throws CreateModelException {
        return playbackSessionStopEventWithReason(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);
    }

    public static PlaybackSessionEvent playbackSessionStopEventWithReason(int stopReason) throws CreateModelException {
        PlaybackSessionEvent previousPlayEvent = playbackSessionPlayEvent();
        return PlaybackSessionEvent.forStop(
                TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(1L), Urn.forUser(2L)),
                Urn.forUser(1), new TrackSourceInfo("screen", false), previousPlayEvent, 0, 1000L, "hls", "playa", "3g", stopReason,
                false);

    }

    public static TrackingEvent unspecifiedTrackingEvent() {
        return new TrackingEvent("test", 123L) {
        };
    }
}

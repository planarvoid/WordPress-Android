package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.tobedevoured.modelcitizen.CreateModelException;

public class TestEvents {

    public static PlaybackSessionEvent playbackSessionPlayEvent() throws CreateModelException {
        return ModelFixtures.create(PlaybackSessionEvent.class);
    }

    public static PlaybackSessionEvent playbackSessionPlayEventWithProgress(int playbackProgress) {
        return PlaybackSessionEvent.forPlay(
                TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(1L)),
                Urn.forUser(1), "hls", new TrackSourceInfo("screen", false), playbackProgress, 1000L);
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
                TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(1L)),
                Urn.forUser(1), "hls", new TrackSourceInfo("screen", false),
                previousPlayEvent, stopReason, 0, 1000L);

    }
}

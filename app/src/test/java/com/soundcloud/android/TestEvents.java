package com.soundcloud.android;

import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;

public class TestEvents {

    public static PlaybackSessionEvent playbackSessionPlayEvent() throws CreateModelException {
        return TestHelper.getModelFactory().createModel(PlaybackSessionEvent.class);
    }

    public static PlaybackSessionEvent playbackSessionPlayEventWithProgress(int playbackProgress) {
        return PlaybackSessionEvent.forPlay(
                TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(1L)),
                Urn.forUser(1), "hls", new TrackSourceInfo("screen", false), playbackProgress, 1000L);
    }

    public static PlaybackSessionEvent playbackSessionStopEvent() throws CreateModelException {
        PlaybackSessionEvent previousPlayEvent = playbackSessionPlayEvent();
        return PlaybackSessionEvent.forStop(
                TestPropertySets.expectedTrackForAnalytics(Urn.forTrack(1L)),
                Urn.forUser(1), "hls", new TrackSourceInfo("screen", false),
                previousPlayEvent, PlaybackSessionEvent.STOP_REASON_BUFFERING, 0, 1000L);

    }
}

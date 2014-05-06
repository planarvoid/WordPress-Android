package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackSessionEventTest {

    long USER_ID = 1L;

    Track track;
    @Mock
    TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp() throws Exception {
        track = TestHelper.getModelFactory().createModel(Track.class);
    }

    @Test
    public void stopEventSetsTimeElapsedSinceLastPlayEvent() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, USER_ID, trackSourceInfo);
        final PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(track, USER_ID, trackSourceInfo, playEvent, PlaybackSessionEvent.STOP_REASON_BUFFERING);
        expect(stopEvent.getListenTime()).toEqual(stopEvent.getTimeStamp() - playEvent.getTimeStamp());
    }

    @Test
    public void stopEventSetsStopReason() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(track, USER_ID, trackSourceInfo);
        final PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(track, USER_ID, trackSourceInfo, playEvent, PlaybackSessionEvent.STOP_REASON_BUFFERING);
        expect(stopEvent.getStopReason()).toEqual(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }
}

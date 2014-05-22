package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserUrn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackSessionEventTest {

    private static final long DURATION = 1000L;
    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);
    private static final UserUrn USER_URN = Urn.forUser(1L);
    @Mock
    TrackSourceInfo trackSourceInfo;

    @Test
    public void stopEventSetsTimeElapsedSinceLastPlayEvent() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_URN, USER_URN, trackSourceInfo, DURATION);
        final PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(TRACK_URN, USER_URN, trackSourceInfo, playEvent, DURATION, PlaybackSessionEvent.STOP_REASON_BUFFERING);
        expect(stopEvent.getListenTime()).toEqual(stopEvent.getTimeStamp() - playEvent.getTimeStamp());
    }

    @Test
    public void stopEventSetsStopReason() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_URN, USER_URN, trackSourceInfo, DURATION);
        final PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(TRACK_URN, USER_URN, trackSourceInfo, playEvent, DURATION, PlaybackSessionEvent.STOP_REASON_BUFFERING);
        expect(stopEvent.getStopReason()).toEqual(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }
}

package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackSessionEventTest {

    private static final int DURATION = 1000;
    private static final long PROGRESS = 12345L;
    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);
    private static final UserUrn USER_URN = Urn.forUser(1L);
    private static final PropertySet TRACK_DATA = PropertySet.from(
            TrackProperty.URN.bind(TRACK_URN),
            TrackProperty.POLICY.bind("allow"),
            PlayableProperty.DURATION.bind(DURATION)
    );

    @Mock TrackSourceInfo trackSourceInfo;

    @Test
    public void stopEventSetsTimeElapsedSinceLastPlayEvent() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, PROGRESS);
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(TRACK_DATA, USER_URN, trackSourceInfo, playEvent,
                PlaybackSessionEvent.STOP_REASON_BUFFERING, PROGRESS);
        expect(stopEvent.getListenTime()).toEqual(stopEvent.getTimeStamp() - playEvent.getTimeStamp());
    }

    @Test
    public void stopEventSetsStopReason() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, PROGRESS);
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(TRACK_DATA, USER_URN, trackSourceInfo, playEvent,
                PlaybackSessionEvent.STOP_REASON_BUFFERING, PROGRESS);
        expect(stopEvent.getStopReason()).toEqual(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }
}

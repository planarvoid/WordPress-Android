package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackSessionEventTest {

    private static final int DURATION = 1000;
    private static final long PROGRESS = 12345L;
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn USER_URN = Urn.forUser(1L);
    private static final String PROTOCOL = "hls";
    private static final PropertySet TRACK_DATA = PropertySet.from(
            TrackProperty.URN.bind(TRACK_URN),
            TrackProperty.POLICY.bind("allow"),
            PlayableProperty.DURATION.bind(DURATION)
    );
    private static final PropertySet AUDIO_AD_DATA = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
    private static final PropertySet AUDIO_AD_TRACK_DATA = TestPropertySets.expectedTrackForPlayer();
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String CONNECTION_TYPE = "CONNECTION";

    @Mock TrackSourceInfo trackSourceInfo;

    @Test
    public void stopEventSetsTimeElapsedSinceLastPlayEvent() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, PROGRESS, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(TRACK_DATA, USER_URN, trackSourceInfo, playEvent, PROGRESS, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_BUFFERING
        );
        expect(stopEvent.getListenTime()).toEqual(stopEvent.getTimestamp() - playEvent.getTimestamp());
    }

    @Test
    public void stopEventSetsStopReason() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, PROGRESS, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(TRACK_DATA, USER_URN, trackSourceInfo, playEvent, PROGRESS, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_BUFFERING
        );
        expect(stopEvent.getStopReason()).toEqual(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }

    @Test
    public void aPlayEventWithProgressZeroIsAFirstPlay() throws Exception {
        long progress = 0L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        expect(playEvent.isFirstPlay()).toBeTrue();
    }

    @Test
    public void aPlayEventWithProgressOtherThanZeroIsNotAFirstPlay() {
        long progress = 1000L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        expect(playEvent.isFirstPlay()).toBeFalse();
    }

    @Test
    public void aStopEventWithProgressZeroIsNotAFirstPlay() {
        long progress = 0L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(TRACK_DATA, USER_URN, trackSourceInfo, playEvent, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED
        );
        expect(stopEvent.isFirstPlay()).toBeFalse();
    }

    @Test
    public void noAdUrnIndicatesNoAd() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, PROGRESS, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        expect(playEvent.isAd()).toBeFalse();
    }

    @Test
    public void anEventWithAnAdUrnIndicatesAnAd() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                AUDIO_AD_TRACK_DATA,
                USER_URN,
                trackSourceInfo, 0L, "hls",
                PLAYER_TYPE,
                CONNECTION_TYPE).withAudioAd(AUDIO_AD_DATA);

        expect(playEvent.isAd()).toBeTrue();
    }

    @Test
    public void shouldPopulateAdAttributesFromAdPlaybackEvent() {
        final PropertySet audioAd = TestPropertySets.audioAdProperties(TRACK_URN);

        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(
                TestPropertySets.expectedTrackForAnalytics(TRACK_URN),
                USER_URN, trackSourceInfo, PROGRESS, 1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE).withAudioAd(audioAd);

        expect(event.isAd()).toBeTrue();
        expect(event.get(AdTrackingKeys.KEY_AD_URN)).toEqual("ad:audio:123");
        expect(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).toEqual(TRACK_URN.toString());
        expect(event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).toEqual(audioAd.get(AdProperty.ARTWORK).toString());
        expect(event.getAudioAdImpressionUrls()).toContain("adswizzUrl", "advertiserUrl");
        expect(event.getAudioAdCompanionImpressionUrls()).toContain("visual1", "visual2");
        expect(event.getAudioAdFinishUrls()).toContain("finish1", "finish2");
    }
}

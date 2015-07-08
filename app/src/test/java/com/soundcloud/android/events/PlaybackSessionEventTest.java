package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Optional;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

public class PlaybackSessionEventTest extends AndroidUnitTest {

    private static final long DURATION = 1000L;
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
        assertThat(stopEvent.getListenTime()).isEqualTo(stopEvent.getTimestamp() - playEvent.getTimestamp());
    }

    @Test
    public void stopEventSetsStopReason() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, PROGRESS, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(TRACK_DATA, USER_URN, trackSourceInfo, playEvent, PROGRESS, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_BUFFERING
        );
        assertThat(stopEvent.getStopReason()).isEqualTo(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }

    @Test
    public void playEventWithNegativeProgressIsNotAFirstPlay() throws Exception {
        long progress = -1L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        assertThat(playEvent.isFirstPlay()).isFalse();
    }

    @Test
    public void playEventWithProgressZeroIsAFirstPlay() throws Exception {
        long progress = 0L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        assertThat(playEvent.isFirstPlay()).isTrue();
    }

    @Test
    public void playEventWithProgress500msIsAFirstPlay() {
        long progress = 500L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        assertThat(playEvent.isFirstPlay()).isTrue();
    }

    @Test
    public void playEventWithProgressGreaterThan500msIsNotAFirstPlay() {
        long progress = PlaybackSessionEvent.FIRST_PLAY_MAX_PROGRESS + 1;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        assertThat(playEvent.isFirstPlay()).isFalse();
    }

    @Test
    public void stopEventWithProgressZeroIsNotAFirstPlay() {
        long progress = 0L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(TRACK_DATA, USER_URN, trackSourceInfo, playEvent, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);
        assertThat(stopEvent.isFirstPlay()).isFalse();
    }

    @Test
    public void noMonetizationTypeIndicatesNoAudioAdOrPromotedTrack() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, trackSourceInfo, PROGRESS, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE);
        assertThat(playEvent.isAd()).isFalse();
        assertThat(playEvent.isPromotedTrack()).isFalse();
    }

    @Test
    public void eventWithAudioAdMonetizationTypeIndicatesAnAudioAd() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                AUDIO_AD_TRACK_DATA,
                USER_URN,
                trackSourceInfo, 0L, "hls",
                PLAYER_TYPE,
                CONNECTION_TYPE).withAudioAd(AUDIO_AD_DATA);

        assertThat(playEvent.isAd()).isTrue();
    }

    @Test
    public void eventWithPromotedMonetizationTypeIndicatesAPromotedTrack() {
        PromotedSourceInfo promotedInfo = new PromotedSourceInfo("ad:urn:123", Urn.forTrack(123L), Optional.<Urn>absent(), Arrays.asList("url"));

        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                AUDIO_AD_TRACK_DATA,
                USER_URN,
                trackSourceInfo, 0L, "hls",
                PLAYER_TYPE,
                CONNECTION_TYPE).withPromotedTrack(promotedInfo);

        assertThat(playEvent.isPromotedTrack()).isTrue();
    }

    @Test
    public void populatesAdAttributesFromAdPlaybackEvent() {
        final PropertySet audioAd = TestPropertySets.audioAdProperties(TRACK_URN);

        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(
                TestPropertySets.expectedTrackForAnalytics(TRACK_URN),
                USER_URN, trackSourceInfo, PROGRESS, 1000L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE).withAudioAd(audioAd);

        assertThat(event.isAd()).isTrue();
        assertThat(event.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo("ad:audio:123");
        assertThat(event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(TRACK_URN.toString());
        assertThat(event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL)).isEqualTo(audioAd.get(AdProperty.ARTWORK).toString());
        assertThat(event.getAudioAdImpressionUrls()).contains("adswizzUrl", "advertiserUrl");
        assertThat(event.getAudioAdCompanionImpressionUrls()).contains("visual1", "visual2");
        assertThat(event.getAudioAdFinishUrls()).contains("finish1", "finish2");
    }
}

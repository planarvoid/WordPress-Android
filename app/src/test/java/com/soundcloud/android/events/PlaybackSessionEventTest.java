package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.mockito.Mock;

import android.support.annotation.NonNull;

import java.util.Arrays;

public class PlaybackSessionEventTest extends AndroidUnitTest {

    private static final long DURATION = 1000L;
    private static final long PROGRESS = 12345L;
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn LOGGED_IN_USER_URN = Urn.forUser(1L);
    private static final Urn CREATOR_URN = Urn.forUser(2L);
    private static final String PROTOCOL = "hls";
    private static final PropertySet TRACK_DATA = TestPropertySets.expectedTrackForAnalytics(TRACK_URN, CREATOR_URN, "allow", DURATION);

    private static final AudioAd AUDIO_AD_DATA = AdFixtures.getAudioAd(Urn.forTrack(123L));
    private static final PropertySet AUDIO_AD_TRACK_DATA = TestPropertySets.expectedTrackForPlayer();
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String CONNECTION_TYPE = "CONNECTION";
    private static final String UUID = "uuid";
    private static final TestDateProvider DATE_PROVIDER = new TestDateProvider();

    @Mock TrackSourceInfo trackSourceInfo;


    @Test
    public void stopEventSetsTimeElapsedSinceLastPlayEvent() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs());

        final PlaybackSessionEventArgs args = createArgs();
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(playEvent, PlaybackSessionEvent.STOP_REASON_BUFFERING, args);
        assertThat(stopEvent.getListenTime()).isEqualTo(stopEvent.getTimestamp() - playEvent.getTimestamp());
    }

    @Test
    public void stopEventSetsStopReason() throws Exception {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs());
        final PlaybackSessionEventArgs args = createArgs();
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(playEvent, PlaybackSessionEvent.STOP_REASON_BUFFERING, args);
        assertThat(stopEvent.getStopReason()).isEqualTo(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }

    @Test
    public void playEventWithNegativeProgressIsNotAFirstPlay() throws Exception {
        long progress = -1L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs(progress, TRACK_DATA, DATE_PROVIDER));
        assertThat(playEvent.isFirstPlay()).isFalse();
    }

    @Test
    public void playEventWithProgressZeroIsAFirstPlay() throws Exception {
        long progress = 0L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs(progress, TRACK_DATA, DATE_PROVIDER));
        assertThat(playEvent.isFirstPlay()).isTrue();
    }

    @Test
    public void playEventWithProgress500msIsAFirstPlay() {
        long progress = 500L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs(progress, TRACK_DATA, DATE_PROVIDER));
        assertThat(playEvent.isFirstPlay()).isTrue();
    }

    @Test
    public void playEventWithProgressGreaterThan500msIsNotAFirstPlay() {
        long progress = PlaybackSessionEvent.FIRST_PLAY_MAX_PROGRESS + 1;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs(progress, TRACK_DATA, DATE_PROVIDER));
        assertThat(playEvent.isFirstPlay()).isFalse();
    }

    @Test
    public void stopEventWithProgressZeroIsNotAFirstPlay() {
        long progress = 0L;
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs(progress, TRACK_DATA, DATE_PROVIDER));
        final PlaybackSessionEventArgs args = createArgs(progress, TRACK_DATA, DATE_PROVIDER);
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(playEvent, PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED, args);
        assertThat(stopEvent.isFirstPlay()).isFalse();
    }

    @Test
    public void noMonetizationTypeIndicatesNoAudioAdOrPromotedTrack() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs());
        assertThat(playEvent.isAd()).isFalse();
        assertThat(playEvent.isPromotedTrack()).isFalse();
    }

    @Test
    public void eventWithAudioAdMonetizationTypeIndicatesAnAudioAd() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(0L, AUDIO_AD_TRACK_DATA, DATE_PROVIDER)).withAudioAd(AUDIO_AD_DATA);

        assertThat(playEvent.isAd()).isTrue();
    }

    @Test
    public void eventWithPromotedMonetizationTypeIndicatesAPromotedTrack() {
        PromotedSourceInfo promotedInfo = new PromotedSourceInfo("ad:urn:123", Urn.forTrack(123L), Optional.<Urn>absent(), Arrays.asList("url"));

        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(0L, AUDIO_AD_TRACK_DATA, DATE_PROVIDER)).withPromotedTrack(promotedInfo);

        assertThat(playEvent.isPromotedTrack()).isTrue();
    }

    @Test
    public void populatesAdAttributesFromAdPlaybackEvent() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);

        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(
                createArgs(PROGRESS, TestPropertySets.expectedTrackForAnalytics(TRACK_URN, CREATOR_URN), new TestDateProvider(1000L))).withAudioAd(audioAd);

        assertThat(event.isAd()).isTrue();
        assertThat(event.get(EventLoggerTrackingKeys.KEY_AD_URN)).isEqualTo("dfp:ads:869");
        assertThat(event.get(EventLoggerTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(TRACK_URN.toString());
        assertThat(event.get(EventLoggerTrackingKeys.KEY_AD_ARTWORK_URL)).isEqualTo(audioAd.getVisualAd().getImageUrl().toString());
        assertThat(event.getAudioAdImpressionUrls()).contains("audio_impression1", "audio_impression2");
        assertThat(event.getAudioAdCompanionImpressionUrls()).contains("comp_impression1", "comp_impression2");
        assertThat(event.getAudioAdFinishUrls()).contains("audio_finish1", "audio_finish2");
    }

    @Test
    public void eventWithMarketablePlayIndicatesMarketablePlay() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(PlaybackSessionEventArgs.create(TRACK_DATA, LOGGED_IN_USER_URN, trackSourceInfo, 0L, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, true, UUID, DATE_PROVIDER));
        assertThat(playEvent.isMarketablePlay()).isTrue();
    }

    @NonNull
    private PlaybackSessionEventArgs createArgs() {
        return createArgs(PROGRESS, TRACK_DATA, DATE_PROVIDER);
    }

    @NonNull
    private PlaybackSessionEventArgs createArgs(long progress, PropertySet trackData, TestDateProvider dateProvider) {
        return PlaybackSessionEventArgs.create(trackData, LOGGED_IN_USER_URN, trackSourceInfo, progress, PROTOCOL, PLAYER_TYPE, CONNECTION_TYPE, false, false, UUID, dateProvider);
    }

}

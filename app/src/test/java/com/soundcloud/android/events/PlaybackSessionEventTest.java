package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.annotation.NonNull;

import java.util.Arrays;

public class PlaybackSessionEventTest extends AndroidUnitTest {

    private static final long DURATION = 1000L;
    private static final long PROGRESS = 12345L;
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn CREATOR_URN = Urn.forUser(2L);
    private static final String PROTOCOL = "hls";
    private static final PropertySet TRACK_DATA = TestPropertySets.expectedTrackForAnalytics(TRACK_URN,
                                                                                             CREATOR_URN,
                                                                                             "allow",
                                                                                             DURATION);

    private static final PropertySet AUDIO_AD_TRACK_DATA = TestPropertySets.expectedTrackForPlayer();
    private static final String PLAYER_TYPE = "PLAYA";
    private static final String UUID = "uuid";

    private AudioAd audioAdData;

    @Mock TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp() {
        audioAdData = AdFixtures.getAudioAd(Urn.forTrack(123L));
    }

    @Test
    public void stopEventSetsTimeElapsedSinceLastPlayEvent() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs());

        final PlaybackSessionEventArgs args = createArgs();
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(playEvent,
                                                                      PlaybackSessionEvent.STOP_REASON_BUFFERING,
                                                                      args);
        assertThat(stopEvent.getListenTime()).isEqualTo(stopEvent.getTimestamp() - playEvent.getTimestamp());
    }

    @Test
    public void stopEventSetsStopReason() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs());
        final PlaybackSessionEventArgs args = createArgs();
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(playEvent,
                                                                      PlaybackSessionEvent.STOP_REASON_BUFFERING,
                                                                      args);
        assertThat(stopEvent.getStopReason()).isEqualTo(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }

    @Test
    public void playEventWithStartNotMarkedAsReportedInAdDataIsFirstAdPlay() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs(1L, TRACK_DATA))
                                                             .withAudioAd(audioAdData);
        assertThat(playEvent.shouldReportAdStart()).isTrue();
    }

    @Test
    public void checkpointEventIsCreated() {
        PlaybackSessionEvent checkpointEvent = PlaybackSessionEvent.forCheckpoint(createArgs());
        assertThat(checkpointEvent.isCheckpointEvent()).isTrue();
    }

    @Test
    public void checkpointEventHasTrackProperties() {
        PlaybackSessionEvent checkpointEvent = PlaybackSessionEvent.forCheckpoint(createArgs());
        assertThat(checkpointEvent.getTrackUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void playEventWithStartMarkedSentAdDataIsNotAFirstPlay() {
        audioAdData.setStartReported();
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs(1L, TRACK_DATA))
                                                             .withAudioAd(audioAdData);
        assertThat(playEvent.shouldReportAdStart()).isFalse();
    }

    @Test
    public void stopEventIsNotAFirstPlay() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(createArgs(0L, TRACK_DATA))
                                                             .withAudioAd(audioAdData);
        final PlaybackSessionEventArgs args = createArgs(0L, TRACK_DATA);
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(playEvent,
                                                                      PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED,
                                                                      args);
        assertThat(stopEvent.shouldReportAdStart()).isFalse();
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
                createArgs(0L, AUDIO_AD_TRACK_DATA)).withAudioAd(audioAdData);

        assertThat(playEvent.isAd()).isTrue();
    }

    @Test
    public void eventWithPromotedMonetizationTypeIndicatesAPromotedTrack() {
        PromotedSourceInfo promotedInfo = new PromotedSourceInfo("ad:urn:123",
                                                                 Urn.forTrack(123L),
                                                                 Optional.<Urn>absent(),
                                                                 Arrays.asList("url"));

        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(0L, AUDIO_AD_TRACK_DATA)).withPromotedTrack(promotedInfo);

        assertThat(playEvent.isPromotedTrack()).isTrue();
    }

    @Test
    public void eventForPromotedTrackReportsAdStartBasedOnSource() {
        PromotedSourceInfo promotedInfo = new PromotedSourceInfo("ad:urn:123",
                                                                 Urn.forTrack(123L),
                                                                 Optional.<Urn>absent(),
                                                                 Arrays.asList("url"));

        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(0L, AUDIO_AD_TRACK_DATA)).withPromotedTrack(promotedInfo);

        assertThat(playEvent.shouldReportAdStart()).isTrue();
    }

    @Test
    public void eventForPromotedTrackDoesNotReportAdStartOnMultiplePlayEventsInSameSession() {
        PromotedSourceInfo promotedInfo = new PromotedSourceInfo("ad:urn:123",
                                                                 Urn.forTrack(123L),
                                                                 Optional.<Urn>absent(),
                                                                 Arrays.asList("url"));
        promotedInfo.setPlaybackStarted();

        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(
                createArgs(0L, AUDIO_AD_TRACK_DATA)).withPromotedTrack(promotedInfo);

        assertThat(playEvent.shouldReportAdStart()).isFalse();
    }

    @Test
    public void populatesAdAttributesFromAdPlaybackEvent() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);

        PlaybackSessionEvent event = PlaybackSessionEvent.forPlay(
                createArgs(PROGRESS, TestPropertySets.expectedTrackForAnalytics(TRACK_URN, CREATOR_URN)))
                                                         .withAudioAd(audioAd);

        assertThat(event.isAd()).isTrue();
        assertThat(event.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo("dfp:ads:869");
        assertThat(event.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(TRACK_URN.toString());
        assertThat(event.get(PlayableTrackingKeys.KEY_AD_ARTWORK_URL)).isEqualTo(audioAd.getVisualAd()
                                                                                        .getImageUrl()
                                                                                        .toString());
        assertThat(event.getAudioAdImpressionUrls()).contains("audio_impression1", "audio_impression2");
        assertThat(event.getAudioAdCompanionImpressionUrls()).contains("comp_impression1", "comp_impression2");
        assertThat(event.getAudioAdFinishUrls()).contains("audio_finish1", "audio_finish2");
    }

    @Test
    public void eventWithMarketablePlayIndicatesMarketablePlay() {
        PlaybackSessionEvent playEvent = PlaybackSessionEvent.forPlay(PlaybackSessionEventArgs.create(TRACK_DATA,
                                                                                                      trackSourceInfo,
                                                                                                      0L,
                                                                                                      PROTOCOL,
                                                                                                      PLAYER_TYPE,
                                                                                                      false,
                                                                                                      true,
                                                                                                      UUID));
        assertThat(playEvent.isMarketablePlay()).isTrue();
    }

    @NonNull
    private PlaybackSessionEventArgs createArgs() {
        return createArgs(PROGRESS, TRACK_DATA);
    }

    @NonNull
    private PlaybackSessionEventArgs createArgs(long progress, PropertySet trackData) {
        return PlaybackSessionEventArgs.create(trackData,
                                               trackSourceInfo,
                                               progress,
                                               PROTOCOL,
                                               PLAYER_TYPE,
                                               false,
                                               false,
                                               UUID);
    }

}

package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.java.optional.Optional;

import org.junit.Before;
import org.junit.Test;

public class AdPlaybackSessionEventTest extends AndroidUnitTest {

    public static final Urn AD_URN = Urn.forAd("dfp", "869");
    public static final Urn AD_URN_2 = Urn.forAd("dfp", "905");
    public static final Urn TRACK_URN = Urn.forTrack(123L);
    private TrackSourceInfo sourceInfo;

    @Before
    public void setUp() throws Exception {
        sourceInfo = new TrackSourceInfo("originScreen", true);
    }

    @Test
    public void shouldCreateEventFromFirstQuartileForAudioAd() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forFirstQuartile(audioAd, sourceInfo);

        assertThat(progressEvent.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.QUARTILE);
        assertThat(progressEvent.clickName().get()).isEqualTo(AdPlaybackSessionEvent.ClickName.FIRST_QUARTILE_TYPE);
        assertThat(progressEvent.monetizableTrackUrn()).isEqualTo(Optional.of(TRACK_URN));
        assertThat(progressEvent.adUrn()).isEqualTo(AD_URN);
        assertThat(progressEvent.monetizationType()).isEqualTo(AdData.MonetizationType.AUDIO);
        assertThat(progressEvent.trackingUrls().get()).containsExactly("audio_quartile1_1", "audio_quartile1_2");
    }

    @Test
    public void shouldCreateEventFromSecondQuartileForAudioAd() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forSecondQuartile(audioAd, sourceInfo);

        assertThat(progressEvent.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.QUARTILE);
        assertThat(progressEvent.clickName().get()).isEqualTo(AdPlaybackSessionEvent.ClickName.SECOND_QUARTILE_TYPE);
        assertThat(progressEvent.monetizableTrackUrn()).isEqualTo(Optional.of(TRACK_URN));
        assertThat(progressEvent.adUrn()).isEqualTo(AD_URN);
        assertThat(progressEvent.monetizationType()).isEqualTo(AdData.MonetizationType.AUDIO);
        assertThat(progressEvent.trackingUrls().get()).containsExactly("audio_quartile2_1", "audio_quartile2_2");
    }

    @Test
    public void shouldCreateEventFromThirdQuartileForAudioAd() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forThirdQuartile(audioAd, sourceInfo);

        assertThat(progressEvent.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.QUARTILE);
        assertThat(progressEvent.clickName().get()).isEqualTo(AdPlaybackSessionEvent.ClickName.THIRD_QUARTILE_TYPE);
        assertThat(progressEvent.monetizableTrackUrn()).isEqualTo(Optional.of(TRACK_URN));
        assertThat(progressEvent.adUrn()).isEqualTo(AD_URN);
        assertThat(progressEvent.monetizationType()).isEqualTo(AdData.MonetizationType.AUDIO);
        assertThat(progressEvent.trackingUrls().get()).containsExactly("audio_quartile3_1", "audio_quartile3_2");
    }

    @Test
    public void shouldCreateEventFromFirstQuartileForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK_URN);
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forFirstQuartile(videoAd, sourceInfo);

        assertThat(progressEvent.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.QUARTILE);
        assertThat(progressEvent.clickName().get()).isEqualTo(AdPlaybackSessionEvent.ClickName.FIRST_QUARTILE_TYPE);
        assertThat(progressEvent.monetizableTrackUrn()).isEqualTo(Optional.of(TRACK_URN));
        assertThat(progressEvent.adUrn()).isEqualTo(AD_URN_2);
        assertThat(progressEvent.monetizationType()).isEqualTo(AdData.MonetizationType.VIDEO);
        assertThat(progressEvent.trackingUrls().get()).containsExactly("video_quartile1_1", "video_quartile1_2");
    }

    @Test
    public void shouldCreateEventFromSecondQuartileForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK_URN);
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forSecondQuartile(videoAd, sourceInfo);

        assertThat(progressEvent.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.QUARTILE);
        assertThat(progressEvent.clickName().get()).isEqualTo(AdPlaybackSessionEvent.ClickName.SECOND_QUARTILE_TYPE);
        assertThat(progressEvent.monetizableTrackUrn()).isEqualTo(Optional.of(TRACK_URN));
        assertThat(progressEvent.adUrn()).isEqualTo(AD_URN_2);
        assertThat(progressEvent.monetizationType()).isEqualTo(AdData.MonetizationType.VIDEO);
        assertThat(progressEvent.trackingUrls().get()).containsExactly("video_quartile2_1", "video_quartile2_2");
    }

    @Test
    public void shouldCreateEventFromThirdQuartileForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK_URN);
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forThirdQuartile(videoAd, sourceInfo);

        assertThat(progressEvent.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.QUARTILE);
        assertThat(progressEvent.clickName().get()).isEqualTo(AdPlaybackSessionEvent.ClickName.THIRD_QUARTILE_TYPE);
        assertThat(progressEvent.monetizableTrackUrn()).isEqualTo(Optional.of(TRACK_URN));
        assertThat(progressEvent.adUrn()).isEqualTo(AD_URN_2);
        assertThat(progressEvent.monetizationType()).isEqualTo(AdData.MonetizationType.VIDEO);
        assertThat(progressEvent.trackingUrls().get()).containsExactly("video_quartile3_1", "video_quartile3_2");
    }

    @Test
    public void shouldCreateFromPlayEventWithImpressionAndStartUrlsForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK_URN);
        final AdSessionEventArgs args = AdSessionEventArgs.create(sourceInfo, TestPlayerTransitions.playing(), "123");
        final AdPlaybackSessionEvent playEvent = AdPlaybackSessionEvent.forStart(videoAd, args);

        assertThat(playEvent.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.START);
        assertThat(playEvent.monetizableTrackUrn()).isEqualTo(Optional.of(TRACK_URN));
        assertThat(playEvent.adUrn()).isEqualTo(AD_URN_2);
        assertThat(playEvent.monetizationType()).isEqualTo(AdData.MonetizationType.VIDEO);
        assertThat(playEvent.trackingUrls().get()).containsExactly("video_impression1",
                                                                   "video_impression2",
                                                                   "video_start1",
                                                                   "video_start2");
    }

    @Test
    public void shouldCreateFromPlayEventWithResumeUrlsForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK_URN);
        videoAd.setEventReported(PlayableAdData.ReportingEvent.START);
        final AdSessionEventArgs args = AdSessionEventArgs.create(sourceInfo, TestPlayerTransitions.playing(), "123");
        final AdPlaybackSessionEvent playEvent = AdPlaybackSessionEvent.forResume(videoAd, args);

        assertThat(playEvent.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.RESUME);
        assertThat(playEvent.monetizableTrackUrn()).isEqualTo(Optional.of(TRACK_URN));
        assertThat(playEvent.adUrn()).isEqualTo(AD_URN_2);
        assertThat(playEvent.monetizationType()).isEqualTo(AdData.MonetizationType.VIDEO);
        assertThat(playEvent.trackingUrls().get()).containsExactly("video_resume1", "video_resume2");
    }


    @Test
    public void shouldCreateFromStopEventWithFinishUrlsForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK_URN);
        final AdSessionEventArgs args = AdSessionEventArgs.create(sourceInfo, TestPlayerTransitions.idle(), "123");
        final AdPlaybackSessionEvent stopEvent = AdPlaybackSessionEvent.forFinish(videoAd, args);

        assertThat(stopEvent.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.FINISH);
        assertThat(stopEvent.monetizableTrackUrn()).isEqualTo(Optional.of(TRACK_URN));
        assertThat(stopEvent.adUrn()).isEqualTo(AD_URN_2);
        assertThat(stopEvent.monetizationType()).isEqualTo(AdData.MonetizationType.VIDEO);
        assertThat(stopEvent.trackingUrls().get()).containsExactly("video_finish1", "video_finish2");
    }

    @Test
    public void shouldCreateFromStopEventWithPauseUrlsForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK_URN);
        final AdSessionEventArgs args = AdSessionEventArgs.create(sourceInfo, TestPlayerTransitions.idle(), "123");
        final AdPlaybackSessionEvent stopEvent = AdPlaybackSessionEvent.forPause(videoAd, args);

        assertThat(stopEvent.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.PAUSE);
        assertThat(stopEvent.monetizableTrackUrn()).isEqualTo(Optional.of(TRACK_URN));
        assertThat(stopEvent.adUrn()).isEqualTo(AD_URN_2);
        assertThat(stopEvent.monetizationType()).isEqualTo(AdData.MonetizationType.VIDEO);
        assertThat(stopEvent.trackingUrls().get()).containsExactly("video_pause1", "video_pause2");
    }
}

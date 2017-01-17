package com.soundcloud.android.events;

import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_PAUSE;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_TRACK_FINISHED;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import org.junit.Before;
import org.junit.Test;

public class AdPlaybackSessionEventTest extends AndroidUnitTest {

    private TrackSourceInfo sourceInfo;

    @Before
    public void setUp() throws Exception {
        sourceInfo = new TrackSourceInfo("originScreen", true);
    }

    @Test
    public void shouldCreateEventFromFirstQuartileForAudioAd() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forFirstQuartile(audioAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::first_quartile");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L)
                                                                                                   .toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "869").toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("audio_ad");
        assertThat(progressEvent.getTrackingUrls()).containsExactly("audio_quartile1_1", "audio_quartile1_2");
    }

    @Test
    public void shouldCreateEventFromSecondQuartileForAudioAd() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forSecondQuartile(audioAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::second_quartile");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L)
                                                                                                   .toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "869").toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("audio_ad");
        assertThat(progressEvent.getTrackingUrls()).containsExactly("audio_quartile2_1", "audio_quartile2_2");
    }

    @Test
    public void shouldCreateEventFromThirdQuartileForAudioAd() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forThirdQuartile(audioAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::third_quartile");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L)
                                                                                                   .toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "869").toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("audio_ad");
        assertThat(progressEvent.getTrackingUrls()).containsExactly("audio_quartile3_1", "audio_quartile3_2");
    }

    @Test
    public void shouldCreateEventFromFirstQuartileForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forFirstQuartile(videoAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::first_quartile");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L)
                                                                                                   .toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "905").toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(progressEvent.getTrackingUrls()).containsExactly("video_quartile1_1", "video_quartile1_2");
    }

    @Test
    public void shouldCreateEventFromSecondQuartileForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forSecondQuartile(videoAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::second_quartile");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L)
                                                                                                   .toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "905").toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(progressEvent.getTrackingUrls()).containsExactly("video_quartile2_1", "video_quartile2_2");
    }

    @Test
    public void shouldCreateEventFromThirdQuartileForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdPlaybackSessionEvent progressEvent = AdPlaybackSessionEvent.forThirdQuartile(videoAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::third_quartile");
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L)
                                                                                                   .toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "905").toString());
        assertThat(progressEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(progressEvent.getTrackingUrls()).containsExactly("video_quartile3_1", "video_quartile3_2");
    }

    @Test
    public void shouldCreateFromPlayEventWithImpressionAndStartUrlsForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdPlaybackSessionEventArgs args = AdPlaybackSessionEventArgs.create(sourceInfo, TestPlayerTransitions.playing(), "123");
        final AdPlaybackSessionEvent playEvent = AdPlaybackSessionEvent.forPlay(videoAd, args);

        assertThat(playEvent.getKind()).isEqualTo("play");
        assertThat(playEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L)
                                                                                               .toString());
        assertThat(playEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "905").toString());
        assertThat(playEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(playEvent.getTrackingUrls()).containsExactly("video_impression1",
                                                                "video_impression2",
                                                                "video_start1",
                                                                "video_start2");
    }

    @Test
    public void shouldCreateFromPlayEventWithResumeUrlsForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        videoAd.setStartReported();
        final AdPlaybackSessionEventArgs args = AdPlaybackSessionEventArgs.create(sourceInfo, TestPlayerTransitions.playing(), "123");
        final AdPlaybackSessionEvent playEvent = AdPlaybackSessionEvent.forPlay(videoAd, args);

        assertThat(playEvent.getKind()).isEqualTo("play");
        assertThat(playEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L)
                                                                                               .toString());
        assertThat(playEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "905").toString());
        assertThat(playEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(playEvent.getTrackingUrls()).containsExactly("video_resume1", "video_resume2");
    }


    @Test
    public void shouldCreateFromStopEventWithFinishUrlsForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdPlaybackSessionEventArgs args = AdPlaybackSessionEventArgs.create(sourceInfo, TestPlayerTransitions.idle(), "123");
        final AdPlaybackSessionEvent stopEvent = AdPlaybackSessionEvent.forStop(videoAd,
                                                                                args,
                                                                                STOP_REASON_TRACK_FINISHED);

        assertThat(stopEvent.getKind()).isEqualTo("stop");
        assertThat(stopEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L)
                                                                                               .toString());
        assertThat(stopEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "905").toString());
        assertThat(stopEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(stopEvent.getTrackingUrls()).containsExactly("video_finish1", "video_finish2");
    }

    @Test
    public void shouldCreateFromStopEventWithPauseUrlsForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdPlaybackSessionEventArgs args = AdPlaybackSessionEventArgs.create(sourceInfo, TestPlayerTransitions.idle(), "123");
        final AdPlaybackSessionEvent stopEvent = AdPlaybackSessionEvent.forStop(videoAd,
                                                                                args,
                                                                                STOP_REASON_PAUSE);

        assertThat(stopEvent.getKind()).isEqualTo("stop");
        assertThat(stopEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L)
                                                                                               .toString());
        assertThat(stopEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "905").toString());
        assertThat(stopEvent.get(PlayableTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(stopEvent.getTrackingUrls()).containsExactly("video_pause1", "video_pause2");
    }
}

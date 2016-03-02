package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AdPlaybackProgressEventTest extends AndroidUnitTest {

    private TrackSourceInfo sourceInfo;

    @Before
    public void setUp() throws Exception {
        sourceInfo = new TrackSourceInfo("originScreen", true);
    }

    @Test
    public void shouldCreateEventFromFirstQuartileForAudioAd() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final AdPlaybackProgressEvent progressEvent = AdPlaybackProgressEvent.forFirstQuartile(Urn.forTrack(321L), audioAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(AdTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::first_quartile");
        assertThat(progressEvent.itemUrn).isEqualTo(Urn.forTrack(321L));
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L).toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "869").toString());

        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("audio_ad");
        assertThat(progressEvent.getQuartileTrackingUrls()).isEmpty();
    }

    @Test
    public void shouldCreateEventFromSecondQuartileForAudioAd() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final AdPlaybackProgressEvent progressEvent = AdPlaybackProgressEvent.forSecondQuartile(Urn.forTrack(321L), audioAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(AdTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::second_quartile");
        assertThat(progressEvent.itemUrn).isEqualTo(Urn.forTrack(321L));
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L).toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "869").toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("audio_ad");
        assertThat(progressEvent.getQuartileTrackingUrls()).isEmpty();
    }

    @Test
    public void shouldCreateEventFromThirdQuartileForAudioAd() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final AdPlaybackProgressEvent progressEvent = AdPlaybackProgressEvent.forThirdQuartile(Urn.forTrack(321L), audioAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(AdTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::third_quartile");
        assertThat(progressEvent.itemUrn).isEqualTo(Urn.forTrack(321L));
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L).toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "869").toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("audio_ad");
        assertThat(progressEvent.getQuartileTrackingUrls()).isEmpty();
    }

    @Test
    public void shouldCreateEventFromFirstQuartileForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdPlaybackProgressEvent progressEvent = AdPlaybackProgressEvent.forFirstQuartile(Urn.forTrack(321L), videoAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(AdTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::first_quartile");
        assertThat(progressEvent.itemUrn).isEqualTo(Urn.forTrack(321L));
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L).toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "905").toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(progressEvent.getQuartileTrackingUrls()).containsExactly("video_quartile1_1", "video_quartile1_2");
    }

    @Test
    public void shouldCreateEventFromSecondQuartileForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdPlaybackProgressEvent progressEvent = AdPlaybackProgressEvent.forSecondQuartile(Urn.forTrack(321L), videoAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(AdTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::second_quartile");
        assertThat(progressEvent.itemUrn).isEqualTo(Urn.forTrack(321L));
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L).toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "905").toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(progressEvent.getQuartileTrackingUrls()).containsExactly("video_quartile2_1", "video_quartile2_2");
    }

    @Test
    public void shouldCreateEventFromThirdQuartileForVideoAd() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdPlaybackProgressEvent progressEvent = AdPlaybackProgressEvent.forThirdQuartile(Urn.forTrack(321L), videoAd, sourceInfo);

        assertThat(progressEvent.getKind()).isEqualTo("quartile_event");
        assertThat(progressEvent.get(AdTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::third_quartile");
        assertThat(progressEvent.itemUrn).isEqualTo(Urn.forTrack(321L));
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(Urn.forTrack(123L).toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(Urn.forAd("dfp", "905").toString());
        assertThat(progressEvent.get(AdTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("video_ad");
        assertThat(progressEvent.getQuartileTrackingUrls()).containsExactly("video_quartile3_1", "video_quartile3_2");
    }
}

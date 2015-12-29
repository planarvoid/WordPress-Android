package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AdFunctionsTest extends AndroidUnitTest {

    @Test
    public void isPlayerAdItemShouldReturnTrueForAudioAdPlayQueueItem() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final TrackQueueItem adItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L), audioAd);
        assertThat(AdFunctions.IS_PLAYER_AD_ITEM.apply(adItem)).isTrue();
    }

    @Test
    public void isPlayerAdItemShouldReturnTrueForVideoAdPlayQueueItem() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final VideoQueueItem adItem = TestPlayQueueItem.createVideo(videoAd);
        assertThat(AdFunctions.IS_PLAYER_AD_ITEM.apply(adItem)).isTrue();
    }

    @Test
    public void isPlayerAdItemShouldReturnFalseForRegularTrackItem() {
        final TrackQueueItem trackItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L));
        assertThat(AdFunctions.IS_PLAYER_AD_ITEM.apply(trackItem)).isFalse();
    }

    @Test
    public void isAudioAdItemShouldReturnTrueForAudioAdPlayQueueItem() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final TrackQueueItem adItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L), audioAd);
        assertThat(AdFunctions.IS_AUDIO_AD_ITEM.apply(adItem)).isTrue();
    }

    @Test
    public void isAudioAdItemShouldReturnFalseForVideoAdPlayQueueItem() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final VideoQueueItem adItem = TestPlayQueueItem.createVideo(videoAd);
        assertThat(AdFunctions.IS_AUDIO_AD_ITEM.apply(adItem)).isFalse();
    }

    @Test
    public void isAudioAdItemShouldReturnFalseForRegularTrackItem() {
        final TrackQueueItem trackItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L));
        assertThat(AdFunctions.IS_AUDIO_AD_ITEM.apply(trackItem)).isFalse();
    }
}

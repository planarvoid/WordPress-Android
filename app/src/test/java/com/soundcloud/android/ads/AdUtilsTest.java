package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioAdQueueItem;
import com.soundcloud.android.playback.AudioPlaybackItem;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoAdQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import org.junit.Test;

public class AdUtilsTest extends AndroidUnitTest {

    @Test
    public void isPlayerAdItemShouldReturnTrueForAudioAdPlayQueueItem() {
        final AudioAdQueueItem adItem = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L)));
        assertThat(AdUtils.IS_PLAYER_AD_ITEM.apply(adItem)).isTrue();
    }

    @Test
    public void isPlayerAdItemShouldReturnTrueForVideoAdPlayQueueItem() {
        final VideoAdQueueItem adItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L)));
        assertThat(AdUtils.IS_PLAYER_AD_ITEM.apply(adItem)).isTrue();
    }

    @Test
    public void isPlayerAdItemShouldReturnFalseForRegularTrackItem() {
        final TrackQueueItem trackItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L));
        assertThat(AdUtils.IS_PLAYER_AD_ITEM.apply(trackItem)).isFalse();
    }

    @Test
    public void isAudioAdItemShouldReturnTrueForAudioAdPlayQueueItem() {
        final AudioAdQueueItem adItem = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L)));
        assertThat(AdUtils.IS_AUDIO_AD_ITEM.apply(adItem)).isTrue();
    }

    @Test
    public void isAudioAdItemShouldReturnFalseForVideoAdPlayQueueItem() {
        final VideoAdQueueItem adItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L)));
        assertThat(AdUtils.IS_AUDIO_AD_ITEM.apply(adItem)).isFalse();
    }

    @Test
    public void isAudioAdItemShouldReturnFalseForRegularTrackItem() {
        final TrackQueueItem trackItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L));
        assertThat(AdUtils.IS_AUDIO_AD_ITEM.apply(trackItem)).isFalse();
    }

    @Test
    public void isAdForPlaybackItemReturnsTrueWhenPlaybackTypeIsAudioAd() {
        PlaybackItem playbackItem = AudioPlaybackItem.create(Urn.forTrack(123L), 0L, 1000L, PlaybackType.AUDIO_AD);
        assertThat(AdUtils.isAd(playbackItem)).isTrue();
    }

    @Test
    public void isAdForPlaybackItemReturnsTrueWhenPlaybackTypeIsVideoDefault() {
        PlaybackItem playbackItem = AudioPlaybackItem.create(Urn.forTrack(123L), 0L, 1000L, PlaybackType.VIDEO_AD);
        assertThat(AdUtils.isAd(playbackItem)).isTrue();
    }

    @Test
    public void isAdForPlaybackItemReturnsTrueWhenUrnIsAd() {
        PlaybackItem playbackItem = AudioPlaybackItem.create(Urn.forAd("totally-an-ad", "yes-it-is"),
                                                             0L,
                                                             1000L,
                                                             PlaybackType.AUDIO_DEFAULT);
        assertThat(AdUtils.isAd(playbackItem)).isTrue();
    }

    @Test
    public void isAdForPlaybackItemReturnsFalseWhenIsANormalAudioItem() {
        PlaybackItem playbackItem = AudioPlaybackItem.create(Urn.forTrack(123L), 0L, 1000L, PlaybackType.AUDIO_DEFAULT);
        assertThat(AdUtils.isAd(playbackItem)).isFalse();
    }

}

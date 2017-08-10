package com.soundcloud.android.ads;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

public class VideoAdTest {

    @Test
    public void isVerticalVideoReturnsTrueIfHeightIsGreaterThanWidth() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L), AdFixtures.getApiVideoSource(100, 500));
        assertThat(videoAd.isVerticalVideo()).isTrue();
    }

    @Test
    public void isVerticalVideoReturnsFalseIfHeightIsEqualToWidth() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L), AdFixtures.getApiVideoSource(100, 100));
        assertThat(videoAd.isVerticalVideo()).isFalse();
    }

    @Test
    public void isVerticalVideoReturnsFalseIfHeightIsLessThanWidth() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L), AdFixtures.getApiVideoSource(400, 250));
        assertThat(videoAd.isVerticalVideo()).isFalse();
    }

}

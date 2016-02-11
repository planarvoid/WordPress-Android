package com.soundcloud.android.ads;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VideoAdTest extends AndroidUnitTest {

    @Test
    public void isVerticalVideoReturnsTrueIfHeightIsGreaterThanWidth() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L), AdFixtures.getApiVideoSource(100, 500, Consts.NOT_SET));
        assertThat(videoAd.isVerticalVideo()).isTrue();
    }

    @Test
    public void isVerticalVideoReturnsFalseIfHeightIsEqualToWidth() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L), AdFixtures.getApiVideoSource(100, 100, Consts.NOT_SET));
        assertThat(videoAd.isVerticalVideo()).isFalse();
    }

    @Test
    public void isVerticalVideoReturnsFalseIfHeightIsLessThanWidth() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L), AdFixtures.getApiVideoSource(400, 250, Consts.NOT_SET));
        assertThat(videoAd.isVerticalVideo()).isFalse();
    }

}

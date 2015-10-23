package com.soundcloud.android.ads;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiVideoAdTest extends AndroidUnitTest {
    private ApiVideoAd videoAd;

    @Before
    public void setUp() {
        videoAd = AdFixtures.getApiVideoAd();
    }

    @Test
    public void shouldConvertToPropertySet() throws Exception {
        final PropertySet properties = videoAd.toPropertySet();

        assertThat(properties.contains(VideoAdProperty.AD_URN)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_TYPE)).isTrue();

        // Companion Ad
        assertThat(properties.contains(VideoAdProperty.COMPANION_URN)).isTrue();
        assertThat(properties.contains(VideoAdProperty.ARTWORK)).isTrue();
        assertThat(properties.contains(VideoAdProperty.CLICK_THROUGH_LINK)).isTrue();
        assertThat(properties.contains(VideoAdProperty.DEFAULT_TEXT_COLOR)).isTrue();
        assertThat(properties.contains(VideoAdProperty.DEFAULT_BACKGROUND_COLOR)).isTrue();
        assertThat(properties.contains(VideoAdProperty.PRESSED_TEXT_COLOR)).isTrue();
        assertThat(properties.contains(VideoAdProperty.PRESSED_BACKGROUND_COLOR)).isTrue();
        assertThat(properties.contains(VideoAdProperty.FOCUSED_TEXT_COLOR)).isTrue();
        assertThat(properties.contains(VideoAdProperty.FOCUSED_BACKGROUND_COLOR)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_CLICKTHROUGH_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_COMPANION_DISPLAY_IMPRESSION_URLS)).isTrue();

        // Tracking Urls
        assertThat(properties.contains(VideoAdProperty.AD_IMPRESSION_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_SKIP_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_START_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_FIRST_QUARTILE_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_SECOND_QUARTILE_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_THIRD_QUARTILE_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_FINISH_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_PAUSE_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_RESUME_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_FULLSCREEN_URLS)).isTrue();
        assertThat(properties.contains(VideoAdProperty.AD_EXIT_FULLSCREEN_URLS)).isTrue();
    }

    @Test
    public void getVideoSourcesReturnsNonEmptyListOfSources() throws Exception {
        assertThat(videoAd.getVideoSources()).isNotEmpty();
    }
}


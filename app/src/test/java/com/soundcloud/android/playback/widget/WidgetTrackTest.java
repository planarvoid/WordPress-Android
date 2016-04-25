package com.soundcloud.android.playback.widget;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;

import android.support.annotation.NonNull;

public class WidgetTrackTest extends AndroidUnitTest {

    private PropertySet trackProperties;

    @Before
    public void setUp() throws Exception {
        trackProperties = TestPropertySets.expectedTrackForWidget();
    }

    @Test
    public void creatorIsEmptyForAudioAd() {
        WidgetTrack viewModel = setupAudioAd();

        assertThat(viewModel.getUserName()).isEqualTo(Strings.EMPTY);
    }

    @Test
    public void creatorIsPopulatedForNormalTrack() {
        WidgetTrack viewModel = setupNormalTrack();

        assertThat(viewModel.getUserName()).isEqualTo(trackProperties.get(PlayableProperty.CREATOR_NAME));
    }

    @Test
    public void titleIsAdvertisementTextForAudioAd() {
        WidgetTrack viewModel = setupAudioAd();

        assertThat(viewModel.getTitle()).isEqualTo(resources().getString(R.string.ads_advertisement));
    }

    @Test
    public void titleIsTrackTitleForNormalTrack() {
        WidgetTrack viewModel = setupNormalTrack();

        assertThat(viewModel.getTitle()).isEqualTo(trackProperties.get(PlayableProperty.TITLE));
    }

    @NonNull
    private WidgetTrack setupAudioAd() {
        trackProperties = TestPropertySets.expectedAudioAdForWidget();
        return new WidgetTrack(resources(), trackProperties);
    }

    @NonNull
    private WidgetTrack setupNormalTrack() {
        trackProperties = TestPropertySets.expectedTrackForWidget();
        return new WidgetTrack(resources(), trackProperties);
    }

}

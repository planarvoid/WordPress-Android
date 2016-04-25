package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;

import android.support.annotation.NonNull;

public class NotificationTrackTest extends AndroidUnitTest {

    private PropertySet trackProperties;

    @Before
    public void setUp() throws Exception {
        trackProperties = TestPropertySets.expectedTrackForPlayer()
                .put(AdProperty.IS_AUDIO_AD, false);
    }

    @Test
    public void creatorIsReplacedWithAdvertisementTextForAudioAd() {
        NotificationTrack viewModel = setupAudioAd();

        assertThat(viewModel.getCreatorName()).isEqualTo(resources().getString(R.string.ads_advertisement));
    }

    @Test
    public void creatorIsPopulatedForNormalTrack() {
        NotificationTrack viewModel = setupNormalTrack();

        assertThat(viewModel.getCreatorName()).isEqualTo(trackProperties.get(PlayableProperty.CREATOR_NAME));
    }

    @Test
    public void titleIsEmptyForAudioAd() {
        NotificationTrack viewModel = setupAudioAd();

        assertThat(viewModel.getTitle()).isEqualTo(Strings.EMPTY);
    }

    @Test
    public void titleIsTrackTitleForNormalTrack() {
        NotificationTrack viewModel = setupNormalTrack();

        assertThat(viewModel.getTitle()).isEqualTo(trackProperties.get(PlayableProperty.TITLE));
    }

    @NonNull
    private NotificationTrack setupAudioAd() {
        trackProperties.put(AdProperty.IS_AUDIO_AD, true);
        return new NotificationTrack(resources(), trackProperties);
    }

    @NonNull
    private NotificationTrack setupNormalTrack() {
        trackProperties.put(AdProperty.IS_AUDIO_AD, false);
        return new NotificationTrack(resources(), trackProperties);
    }

}

package com.soundcloud.android.playback;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationTrackTest extends AndroidUnitTest {
    private PropertySet trackProperties;

    @Before
    public void setUp() throws Exception {
        trackProperties = TestPropertySets.expectedTrackForPlayer()
                .put(AdProperty.IS_AUDIO_AD, false);
    }

    @Test
    public void creatorNameShouldBeAdvertisementWhenTrackIsAnAd() {
        trackProperties.put(AdProperty.IS_AUDIO_AD, true);
        com.soundcloud.android.playback.NotificationTrack viewModel = new com.soundcloud.android.playback.NotificationTrack(resources(), trackProperties);

        assertThat(viewModel.getCreatorName()).isEqualTo(resources().getString(R.string.ads_advertisement));
    }

    @Test
    public void returnTrackUserNameIfIsNormalTrack() {
        com.soundcloud.android.playback.NotificationTrack viewModel = new com.soundcloud.android.playback.NotificationTrack(resources(), trackProperties);

        assertThat(viewModel.getCreatorName()).isEqualTo(trackProperties.get(PlayableProperty.CREATOR_NAME));
    }

}
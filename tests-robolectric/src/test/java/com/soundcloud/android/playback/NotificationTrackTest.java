package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class NotificationTrackTest {
    private PropertySet trackProperties;
    private PropertySet adProperties;
    private Resources resources;

    @Before
    public void setUp() throws Exception {
        trackProperties = TestPropertySets.expectedTrackForPlayer();
        adProperties = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        resources = Robolectric.application.getResources();
    }

    @Test
    public void creatorNameShouldBeAdvertisementWhenTrackIsAnAd() {
        NotificationTrack viewModel = new NotificationTrack(resources, trackProperties.merge(adProperties));

        expect(viewModel.getCreatorName()).toEqual(resources.getString(R.string.advertisement));
    }

    @Test
    public void returnTrackUserNameIfIsNormalTrack() {
        NotificationTrack viewModel = new NotificationTrack(resources, trackProperties);

        expect(viewModel.getCreatorName()).toEqual(trackProperties.get(PlayableProperty.CREATOR_NAME));
    }

}
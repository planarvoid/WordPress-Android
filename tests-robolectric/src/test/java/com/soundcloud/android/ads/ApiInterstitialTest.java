package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiInterstitialTest {

    @Test
    public void shouldResolveToPropertySet() throws CreateModelException {
        ApiInterstitial apiInterstitial = ModelFixtures.create(ApiInterstitial.class);

        final PropertySet propertySet = apiInterstitial.toPropertySet();

        expect(propertySet.contains(InterstitialProperty.INTERSTITIAL_URN)).toBeTrue();
        expect(propertySet.contains(InterstitialProperty.IMAGE_URL)).toBeTrue();
        expect(propertySet.contains(InterstitialProperty.CLICK_THROUGH_URL)).toBeTrue();
        expect(propertySet.contains(InterstitialProperty.TRACKING_CLICK_URLS)).toBeTrue();
        expect(propertySet.contains(InterstitialProperty.TRACKING_IMPRESSION_URLS)).toBeTrue();
    }

}

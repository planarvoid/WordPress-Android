package com.soundcloud.android.configuration.features;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ScTestSharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class FeatureOperationsTest {

    private FeatureOperations featureOperations;

    @Before
    public void setUp() throws Exception {
        FeatureStorage storage = new FeatureStorage(new ScTestSharedPreferences());
        featureOperations = new FeatureOperations(storage);
    }

    @Test
    public void isOfflineContentEnabledReturnsDownloadedState() {
        featureOperations.update("offline_sync", true);

        expect(featureOperations.isOfflineContentEnabled()).toBeTrue();
    }

    @Test
    public void isOfflineContentEnabledDefaultsFalse() {
        expect(featureOperations.isOfflineContentEnabled()).toBeFalse();
    }

    @Test
    public void isOfflineContentUpsellEnabledReturnsDownloadedState() {
        featureOperations.update("offline_sync_upsell", true);

        expect(featureOperations.isOfflineContentUpsellEnabled()).toBeTrue();
    }

    @Test
    public void isOfflineContentUpsellEnabledDefaultsFalse() {
        expect(featureOperations.isOfflineContentUpsellEnabled()).toBeFalse();
    }

}
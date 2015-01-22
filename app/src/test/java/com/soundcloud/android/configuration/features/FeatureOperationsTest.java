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
    public void isOfflineSyncEnabledReturnsDownloadedState() {
        featureOperations.update("offline_sync", true);

        expect(featureOperations.isOfflineSyncEnabled()).toBeTrue();
    }

    @Test
    public void isOfflineSyncEnabledDefaultsFalse() {
        expect(featureOperations.isOfflineSyncEnabled()).toBeFalse();
    }

    @Test
    public void isOfflineSyncUpsellEnabledReturnsDownloadedState() {
        featureOperations.update("offline_sync_upsell", true);

        expect(featureOperations.isOfflineSyncUpsellEnabled()).toBeTrue();
    }

    @Test
    public void isOfflineSyncUpsellEnabledDefaultsFalse() {
        expect(featureOperations.isOfflineSyncUpsellEnabled()).toBeFalse();
    }

}
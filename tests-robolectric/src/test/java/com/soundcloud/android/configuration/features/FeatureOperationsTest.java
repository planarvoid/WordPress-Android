package com.soundcloud.android.configuration.features;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class FeatureOperationsTest {

    @Mock private ApplicationProperties appProperties;
    @Mock private FeatureStorage featureStorage;

    private FeatureOperations featureOperations;

    @Before
    public void setUp() throws Exception {
        when(appProperties.isAlphaBuild()).thenReturn(false);
        featureOperations = new FeatureOperations(appProperties, featureStorage);
    }

    @Test
    public void isOfflineContentEnabledReturnsStoredState() {
        when(featureStorage.isEnabled("offline_sync", false)).thenReturn(true);

        expect(featureOperations.isOfflineContentEnabled()).toBeTrue();
    }

    @Test
    public void isOfflineContentEnabledDefaultsFalse() {
        expect(featureOperations.isOfflineContentEnabled()).toBeFalse();
    }

    @Test
    public void isOfflineContentUpsellEnabledReturnsStoredState() {
        when(featureStorage.isEnabled("offline_sync_upsell", false)).thenReturn(true);

        expect(featureOperations.isOfflineContentUpsellEnabled()).toBeTrue();
    }

    @Test
    public void isOfflineContentUpsellEnabledDefaultsFalse() {
        expect(featureOperations.isOfflineContentUpsellEnabled()).toBeFalse();
    }

}
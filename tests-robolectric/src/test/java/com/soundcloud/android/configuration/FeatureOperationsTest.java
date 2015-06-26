package com.soundcloud.android.configuration;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class FeatureOperationsTest {

    @Mock private FeatureStorage featureStorage;
    @Mock private PlanStorage planStorage;

    private FeatureOperations featureOperations;

    @Before
    public void setUp() throws Exception {
        featureOperations = new FeatureOperations(featureStorage, planStorage);
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
    public void shouldShowUpsellIfSetToKnownPlan() {
        when(planStorage.get(eq("upsell"), anyString())).thenReturn("mid_tier");

        expect(featureOperations.upsellMidTier()).toBeTrue();
    }

    @Test
    public void clearsStoredUpsellIfNoneIsReturned() {
        featureOperations.updatePlan("none", null);

        verify(planStorage).remove("upsell");
    }

    @Test
    public void shouldShowUpsellDefaultsFalse() {
        expect(featureOperations.upsellMidTier()).toBeFalse();
    }

    @Test
    public void getPlanReturnsStoredPlan() {
        when(planStorage.get(eq("plan"), anyString())).thenReturn("mid_tier");

        expect(featureOperations.getPlan()).toEqual("mid_tier");
    }

}

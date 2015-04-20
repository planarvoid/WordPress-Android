package com.soundcloud.android.configuration;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.features.FeatureStorage;
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
    @Mock private PlanStorage planStorage;

    private FeatureOperations featureOperations;

    @Before
    public void setUp() throws Exception {
        when(appProperties.isAlphaBuild()).thenReturn(false);
        featureOperations = new FeatureOperations(appProperties, featureStorage, planStorage);
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

        expect(featureOperations.shouldShowUpsell()).toBeTrue();
    }

    @Test
    public void clearsStoredUpsellIfNoneIsReturned() {
        featureOperations.updatePlan("free", null);

        verify(planStorage).remove("upsell");
    }

    @Test
    public void shouldShowUpsellDefaultsFalse() {
        expect(featureOperations.shouldShowUpsell()).toBeFalse();
    }

    @Test
    public void getPlanReturnsStoredPlan() {
        when(planStorage.get(eq("plan"), anyString())).thenReturn("mid_tier");

        expect(featureOperations.getPlan()).toEqual("mid_tier");
    }

}

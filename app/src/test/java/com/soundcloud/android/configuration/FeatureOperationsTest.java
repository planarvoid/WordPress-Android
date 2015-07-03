package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.properties.ApplicationProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
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

        assertThat(featureOperations.isOfflineContentEnabled()).isTrue();
    }

    @Test
    public void isOfflineContentEnabledDefaultsFalse() {
        assertThat(featureOperations.isOfflineContentEnabled()).isFalse();
    }

    @Test
    public void upsellMidTierIfUpsellAvailable() {
        when(planStorage.getList(FeatureOperations.UPSELLS)).thenReturn(Arrays.asList("mid_tier"));

        assertThat(featureOperations.upsellMidTier()).isTrue();
    }

    @Test
    public void upsellMidTierDefaultsFalse() {
        assertThat(featureOperations.upsellMidTier()).isFalse();
    }

    @Test
    public void upsellOfflineContentIfAvailableForMidTierAndMidTierIsAvailable() {
        when(featureStorage.isEnabled("offline_sync", false)).thenReturn(false);
        when(featureStorage.getPlans("offline_sync")).thenReturn(Arrays.asList("mid_tier"));
        when(planStorage.getList(FeatureOperations.UPSELLS)).thenReturn(Arrays.asList("mid_tier"));

        assertThat(featureOperations.upsellOfflineContent()).isTrue();
    }

    @Test
    public void doNotUpsellOfflineContentIfAvailableForMidTierButMidTierIsNotAvailable() {
        when(featureStorage.isEnabled("offline_sync", false)).thenReturn(false);
        when(featureStorage.getPlans("offline_sync")).thenReturn(Arrays.asList("mid_tier"));
        when(planStorage.getList(FeatureOperations.UPSELLS)).thenReturn(new ArrayList<String>());

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void doNotUpsellOfflineContentIfUnavailableForMidTier() {
        when(featureStorage.isEnabled("offline_sync", false)).thenReturn(false);
        when(featureStorage.getPlans("offline_sync")).thenReturn(new ArrayList<String>());
        when(planStorage.getList(FeatureOperations.UPSELLS)).thenReturn(Arrays.asList("mid_tier"));

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void doNotUpsellOfflineContentIfAlreadyEnabled() {
        when(featureStorage.isEnabled("offline_sync", false)).thenReturn(true);
        when(featureStorage.getPlans("offline_sync")).thenReturn(Arrays.asList("mid_tier"));

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void getPlanReturnsStoredPlan() {
        when(planStorage.get(eq("plan"), anyString())).thenReturn("mid_tier");

        assertThat(featureOperations.getPlan()).isEqualTo("mid_tier");
    }

}

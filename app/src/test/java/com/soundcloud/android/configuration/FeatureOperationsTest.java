package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.features.FeatureStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
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
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(true);

        assertThat(featureOperations.isOfflineContentEnabled()).isTrue();
    }

    @Test
    public void isOfflineContentOrUpsellEnabledIfOfflineContentEnabled() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(true);

        assertThat(featureOperations.isOfflineContentOrUpsellEnabled()).isTrue();
    }

    @Test
    public void isOfflineContentEnabledDefaultsFalse() {
        assertThat(featureOperations.isOfflineContentEnabled()).isFalse();
    }

    @Test
    public void upsellHighTierIfUpsellAvailable() {
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.HIGH_TIER));

        assertThat(featureOperations.upsellHighTier()).isTrue();
    }

    @Test
    public void isOfflineContentOrUpsellEnabledFalseByDefault() {
        assertThat(featureOperations.isOfflineContentOrUpsellEnabled()).isFalse();
    }

    @Test
    public void upsellHighTierDefaultsFalse() {
        assertThat(featureOperations.upsellHighTier()).isFalse();
    }

    @Test
    public void upsellOfflineContentIfAvailableForHighTierAndHighTierIsAvailable() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(Arrays.asList(Plan.HIGH_TIER));
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.HIGH_TIER));

        assertThat(featureOperations.upsellOfflineContent()).isTrue();
    }

    @Test
    public void isOfflineContentOrUpsellEnabledIfAvailableForHighTierAndHighTierIsAvailable() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(Arrays.asList(Plan.HIGH_TIER));
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.HIGH_TIER));

        assertThat(featureOperations.isOfflineContentOrUpsellEnabled()).isTrue();
    }


    @Test
    public void doNotUpsellOfflineContentIfAvailableForHighTierButHighTierIsNotAvailable() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(Arrays.asList(Plan.HIGH_TIER));
        when(planStorage.getUpsells()).thenReturn(new ArrayList<Plan>());

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void doNotUpsellOfflineContentIfUnavailableForHighTier() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(new ArrayList<Plan>());
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.HIGH_TIER));

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void doNotUpsellOfflineContentIfAlreadyEnabled() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(true);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(Arrays.asList(Plan.HIGH_TIER));

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void upsellRemoveAdsIfAvailableForHighTierAndHighTierIsAvailable() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.REMOVE_AUDIO_ADS)).thenReturn(Arrays.asList(Plan.HIGH_TIER));
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.HIGH_TIER));

        assertThat(featureOperations.upsellRemoveAudioAds()).isTrue();
    }

    @Test
    public void doNotUpsellRemoveAdsIfAvailableForHighTierButHighTierIsNotAvailable() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.REMOVE_AUDIO_ADS)).thenReturn(Arrays.asList(Plan.HIGH_TIER));
        when(planStorage.getUpsells()).thenReturn(new ArrayList<Plan>());

        assertThat(featureOperations.upsellRemoveAudioAds()).isFalse();
    }

    @Test
    public void doNotUpsellRemoveAdsIfUnavailableForHighTier() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.REMOVE_AUDIO_ADS)).thenReturn(new ArrayList<Plan>());
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.HIGH_TIER));

        assertThat(featureOperations.upsellRemoveAudioAds()).isFalse();
    }

    @Test
    public void doNotUpsellRemoveAdsIfAlreadyEnabled() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(true);
        when(featureStorage.getPlans(FeatureName.REMOVE_AUDIO_ADS)).thenReturn(Arrays.asList(Plan.HIGH_TIER));

        assertThat(featureOperations.upsellRemoveAudioAds()).isFalse();
    }

    @Test
    public void getPlanReturnsStoredPlan() {
        when(planStorage.getPlan()).thenReturn(Plan.HIGH_TIER);

        assertThat(featureOperations.getPlan()).isEqualTo(Plan.HIGH_TIER);
    }

}

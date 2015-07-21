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
    public void isOfflineContentEnabledDefaultsFalse() {
        assertThat(featureOperations.isOfflineContentEnabled()).isFalse();
    }

    @Test
    public void upsellMidTierIfUpsellAvailable() {
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.MID_TIER));

        assertThat(featureOperations.upsellMidTier()).isTrue();
    }

    @Test
    public void upsellMidTierDefaultsFalse() {
        assertThat(featureOperations.upsellMidTier()).isFalse();
    }

    @Test
    public void upsellOfflineContentIfAvailableForMidTierAndMidTierIsAvailable() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(Arrays.asList(Plan.MID_TIER));
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.MID_TIER));

        assertThat(featureOperations.upsellOfflineContent()).isTrue();
    }

    @Test
    public void doNotUpsellOfflineContentIfAvailableForMidTierButMidTierIsNotAvailable() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(Arrays.asList(Plan.MID_TIER));
        when(planStorage.getUpsells()).thenReturn(new ArrayList<String>());

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void doNotUpsellOfflineContentIfUnavailableForMidTier() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(new ArrayList<String>());
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.MID_TIER));

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void doNotUpsellOfflineContentIfAlreadyEnabled() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(true);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(Arrays.asList(Plan.MID_TIER));

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void upsellRemoveAdsIfAvailableForMidTierAndMidTierIsAvailable() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.REMOVE_AUDIO_ADS)).thenReturn(Arrays.asList(Plan.MID_TIER));
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.MID_TIER));

        assertThat(featureOperations.upsellRemoveAudioAds()).isTrue();
    }

    @Test
    public void doNotUpsellRemoveAdsIfAvailableForMidTierButMidTierIsNotAvailable() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.REMOVE_AUDIO_ADS)).thenReturn(Arrays.asList(Plan.MID_TIER));
        when(planStorage.getUpsells()).thenReturn(new ArrayList<String>());

        assertThat(featureOperations.upsellRemoveAudioAds()).isFalse();
    }

    @Test
    public void doNotUpsellRemoveAdsIfUnavailableForMidTier() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.REMOVE_AUDIO_ADS)).thenReturn(new ArrayList<String>());
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.MID_TIER));

        assertThat(featureOperations.upsellRemoveAudioAds()).isFalse();
    }

    @Test
    public void doNotUpsellRemoveAdsIfAlreadyEnabled() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(true);
        when(featureStorage.getPlans(FeatureName.REMOVE_AUDIO_ADS)).thenReturn(Arrays.asList(Plan.MID_TIER));

        assertThat(featureOperations.upsellRemoveAudioAds()).isFalse();
    }

    @Test
    public void getPlanReturnsStoredPlan() {
        when(planStorage.getPlan()).thenReturn(Plan.MID_TIER);

        assertThat(featureOperations.getPlan()).isEqualTo(Plan.MID_TIER);
    }

}

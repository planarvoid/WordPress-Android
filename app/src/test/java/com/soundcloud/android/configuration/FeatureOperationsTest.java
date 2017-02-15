package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class FeatureOperationsTest {

    @Mock private FeatureStorage featureStorage;
    @Mock private PlanStorage planStorage;
    @Mock private ApplicationProperties applicationProperties;

    private FeatureOperations featureOperations;

    @Before
    public void setUp() throws Exception {
        featureOperations = new FeatureOperations(featureStorage, planStorage, applicationProperties);
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
    public void upsellHighTierAndMidTierIfUpsellsAvailable() {
        when(planStorage.getUpsells()).thenReturn(Arrays.asList(Plan.HIGH_TIER, Plan.MID_TIER));

        assertThat(featureOperations.upsellHighTierAndMidTier()).isTrue();
    }

    @Test
    public void upsellHighTierIfUpsellAvailable() {
        when(planStorage.getUpsells()).thenReturn(Collections.singletonList(Plan.HIGH_TIER));

        assertThat(featureOperations.upsellHighTier()).isTrue();
    }

    @Test
    public void shouldUseKruxForAdTargetingIsDisabledByDefault() {
        assertThat(featureOperations.shouldUseKruxForAdTargeting()).isFalse();
    }

    @Test
    public void shouldUseKruxForAdTargetingIsDisabledWhenStorageContainsFeatureFalse() {
        when(featureStorage.isEnabled(FeatureName.KRUX_ADS, false)).thenReturn(false);
        assertThat(featureOperations.shouldUseKruxForAdTargeting()).isFalse();
    }

    @Test
    public void shouldUseKruxForAdTargetingIsEnabledWhenStorageContainsFeatureTrue() {
        when(featureStorage.isEnabled(FeatureName.KRUX_ADS, false)).thenReturn(true);
        assertThat(featureOperations.shouldUseKruxForAdTargeting()).isTrue();
    }

    @Test
    public void shouldRequestAdsIsTrueByDefault() {
        assertThat(featureOperations.shouldRequestAds()).isTrue();
    }

    @Test
    public void shouldRequestAdsIsTrueWhenStorageContainsFeatureFalse() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(false);
        assertThat(featureOperations.shouldRequestAds()).isTrue();
    }

    @Test
    public void shouldRequestAdsIsFalseWhenStorageContainsFeatureTrue() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(true);
        assertThat(featureOperations.shouldRequestAds()).isFalse();
    }

    public void upsellHighTierAndMidTierDefaultsFalse() {
        assertThat(featureOperations.upsellHighTierAndMidTier()).isFalse();
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
    public void doNotUpsellOfflineContentIfAvailableForHighTierButHighTierIsNotAvailable() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(Arrays.asList(Plan.HIGH_TIER));
        when(planStorage.getUpsells()).thenReturn(new ArrayList<>());

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void doNotUpsellOfflineContentIfUnavailableForHighTier() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.OFFLINE_SYNC)).thenReturn(new ArrayList<>());

        assertThat(featureOperations.upsellOfflineContent()).isFalse();
    }

    @Test
    public void doNotUpsellOfflineContentIfAlreadyEnabled() {
        when(featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false)).thenReturn(true);

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
        when(planStorage.getUpsells()).thenReturn(new ArrayList<>());

        assertThat(featureOperations.upsellRemoveAudioAds()).isFalse();
    }

    @Test
    public void doNotUpsellRemoveAdsIfUnavailableForHighTier() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(false);
        when(featureStorage.getPlans(FeatureName.REMOVE_AUDIO_ADS)).thenReturn(new ArrayList<>());

        assertThat(featureOperations.upsellRemoveAudioAds()).isFalse();
    }

    @Test
    public void doNotUpsellRemoveAdsIfAlreadyEnabled() {
        when(featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)).thenReturn(true);

        assertThat(featureOperations.upsellRemoveAudioAds()).isFalse();
    }

    @Test
    public void getPlanReturnsStoredPlan() {
        when(planStorage.getPlan()).thenReturn(Plan.HIGH_TIER);

        assertThat(featureOperations.getCurrentPlan()).isEqualTo(Plan.HIGH_TIER);
    }

    @Test
    public void shouldReturnTrueIfPlanIsManageable() {
        when(planStorage.isManageable()).thenReturn(true);

        assertThat(featureOperations.isPlanManageable()).isTrue();
    }

    @Test
    public void shouldReturnFalseIfPlanIsNotManageable() {
        when(planStorage.isManageable()).thenReturn(false);

        assertThat(featureOperations.isPlanManageable()).isFalse();
    }

    @Test
    public void shouldReturnTrueIfPlanVendorIsApple() {
        when(planStorage.getVendor()).thenReturn("apple");

        assertThat(featureOperations.isPlanVendorApple()).isTrue();
    }

    @Test
    public void shouldReturnFalseIfPlanVendorIsNotApple() {
        when(planStorage.getVendor()).thenReturn(Strings.EMPTY);

        assertThat(featureOperations.isPlanVendorApple()).isFalse();
    }

    @Test
    public void isHighTierTrialEligibleIsFalseIfTrialDaysIsZero() {
        when(planStorage.getHighTierTrialDays()).thenReturn(0);

        assertThat(featureOperations.isHighTierTrialEligible()).isFalse();
    }

    @Test
    public void isHighTierTrialEligibleIsTrueIfTrialDaysIsNonZero() {
        when(planStorage.getHighTierTrialDays()).thenReturn(30);

        assertThat(featureOperations.isHighTierTrialEligible()).isTrue();
    }

    @Test
    public void developmentMenuDisabledByDefault() {
        assertThat(featureOperations.isDevelopmentMenuEnabled()).isFalse();
    }

    @Test
    public void developmentMenuEnabledWhenStorageContainsFeatureTrue() {
        when(featureStorage.isEnabled(FeatureName.DEVELOPMENT_MENU, false)).thenReturn(true);
        assertThat(featureOperations.isDevelopmentMenuEnabled()).isTrue();
    }

    @Test
    public void developmentMenuEnabledWhenStorageContainsFeatureFalse() {
        when(featureStorage.isEnabled(FeatureName.DEVELOPMENT_MENU, false)).thenReturn(false);
        assertThat(featureOperations.isDevelopmentMenuEnabled()).isFalse();
    }

    @Test
    public void developmentMenuEnabledWhenDebug() {
        when(featureStorage.isEnabled(FeatureName.DEVELOPMENT_MENU, true)).thenReturn(true);
        when(applicationProperties.isDevelopmentMode()).thenReturn(true);
        assertThat(featureOperations.isDevelopmentMenuEnabled()).isTrue();
    }

    @Test
    public void developmentMenuDisabledWhenNotDebug() {
        when(applicationProperties.isDevelopmentMode()).thenReturn(false);
        assertThat(featureOperations.isDevelopmentMenuEnabled()).isFalse();
    }

}

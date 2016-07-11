package com.soundcloud.android.configuration;

import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.properties.ApplicationProperties;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class FeatureOperations {

    private final FeatureStorage featureStorage;
    private final PlanStorage planStorage;
    private final ApplicationProperties applicationProperties;

    @Inject
    public FeatureOperations(FeatureStorage featureStorage,
                             PlanStorage planStorage,
                             ApplicationProperties applicationProperties) {
        this.featureStorage = featureStorage;
        this.planStorage = planStorage;
        this.applicationProperties = applicationProperties;
    }

    public void updateFeatures(List<Feature> features) {
        featureStorage.update(features);
    }

    public void updatePlan(UserPlan userPlan) {
        planStorage.updatePlan(userPlan.currentPlan);
        planStorage.updateUpsells(userPlan.upsells);
    }

    public Plan getCurrentPlan() {
        return planStorage.getPlan();
    }

    public boolean isDevelopmentMenuEnabled() {
        return featureStorage.isEnabled(FeatureName.DEVELOPMENT_MENU, applicationProperties.isDebugBuild());
    }

    public boolean isOfflineContentOrUpsellEnabled() {
        return isOfflineContentEnabled() || upsellOfflineContent();
    }

    public boolean isOfflineContentEnabled() {
        return featureStorage.isEnabled(FeatureName.OFFLINE_SYNC, false);
    }

    public boolean upsellOfflineContent() {
        return !isOfflineContentEnabled()
                && isFeatureAvailableViaUpgrade(FeatureName.OFFLINE_SYNC);
    }

    public boolean upsellRemoveAudioAds() {
        return !featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)
                && isFeatureAvailableViaUpgrade(FeatureName.REMOVE_AUDIO_ADS);
    }

    private boolean isFeatureAvailableViaUpgrade(String featureName) {
        return isFeatureAvailableInPlan(featureName, Plan.HIGH_TIER)
                && upsellHighTier();
    }

    private boolean isFeatureAvailableInPlan(String featureName, Plan plan) {
        return featureStorage.getPlans(featureName).contains(plan);
    }

    public boolean upsellHighTier() {
        return planStorage.getUpsells().contains(Plan.HIGH_TIER);
    }

    public Observable<Boolean> offlineContentEnabled() {
        return getUpdates(FeatureName.OFFLINE_SYNC);
    }

    private Observable<Boolean> getUpdates(String name) {
        return featureStorage.getUpdates(name);
    }

    public Observable<Boolean> developmentMenuEnabled() {
        return getUpdates(FeatureName.DEVELOPMENT_MENU);
    }

}

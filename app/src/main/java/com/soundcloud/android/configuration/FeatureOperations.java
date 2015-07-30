package com.soundcloud.android.configuration;

import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.configuration.features.FeatureStorage;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class FeatureOperations {

    private final FeatureStorage featureStorage;
    private final PlanStorage planStorage;

    @Inject
    public FeatureOperations(FeatureStorage featureStorage, PlanStorage planStorage) {
        this.featureStorage = featureStorage;
        this.planStorage = planStorage;
    }

    public void updateFeatures(List<Feature> features) {
        featureStorage.update(features);
    }

    public void updatePlan(String plan, List<String> upsells) {
        planStorage.updatePlan(plan);
        planStorage.updateUpsells(upsells);
    }

    public String getPlan() {
        return planStorage.getPlan();
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
        return isFeatureAvailableInPlan(featureName, Plan.MID_TIER)
                && upsellMidTier();
    }

    private boolean isFeatureAvailableInPlan(String featureName, String plan) {
        return featureStorage.getPlans(featureName).contains(plan);
    }

    public boolean upsellMidTier() {
        return planStorage.getUpsells().contains(Plan.MID_TIER);
    }

    public Observable<Boolean> offlineContentEnabled() {
         return getUpdates(FeatureName.OFFLINE_SYNC);
    }

    private Observable<Boolean> getUpdates(String name) {
        return featureStorage.getUpdates(name);
    }

}

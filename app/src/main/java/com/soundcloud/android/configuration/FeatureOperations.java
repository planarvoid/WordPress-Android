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
                && featureStorage.getPlans(FeatureName.OFFLINE_SYNC).contains(Plan.MID_TIER)
                && planStorage.getUpsells().contains(Plan.MID_TIER);
    }

    public boolean upsellRemoveAudioAds() {
        return !featureStorage.isEnabled(FeatureName.REMOVE_AUDIO_ADS, false)
                && featureStorage.getPlans(FeatureName.REMOVE_AUDIO_ADS).contains(Plan.MID_TIER)
                && planStorage.getUpsells().contains(Plan.MID_TIER);
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

package com.soundcloud.android.configuration;

import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.properties.ApplicationProperties;
import rx.Observable;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FeatureOperations {

    private static final String NO_PLAN = "none";
    private static final String MID_TIER = "mid_tier";

    // Features
    private static final String OFFLINE_CONTENT = "offline_sync";

    // Plan
    private static final String PLAN = "plan";
    private static final String UPSELLS = "upsells";

    private final FeatureStorage featureStorage;
    private final PlanStorage planStorage;

    @Inject
    public FeatureOperations(ApplicationProperties appProperties, FeatureStorage featureStorage, PlanStorage planStorage) {
        this.featureStorage = featureStorage;
        this.planStorage = planStorage;
        if (appProperties.isAlphaBuild()) {
            updateFeature(OFFLINE_CONTENT, true);
        }
    }

    public void updateFeatures(Map<String, Boolean> features) {
        featureStorage.update(features);
    }

    public void updateFeature(String name, boolean value) {
        featureStorage.update(name, value);
    }

    public List<String> listFeatures() {
        return Arrays.asList(OFFLINE_CONTENT);
    }

    public boolean isFeatureEnabled(String name) {
        return featureStorage.isEnabled(name, false);
    }

    public void updatePlan(String plan, List<String> upsells) {
        planStorage.update(PLAN, plan);
        planStorage.update(UPSELLS, upsells);
    }

    public String getPlan() {
        return planStorage.get(PLAN, NO_PLAN);
    }

    public boolean isOfflineContentEnabled() {
        return featureStorage.isEnabled(OFFLINE_CONTENT, false);
    }

    public boolean upsellMidTier() {
        return planStorage.getList(UPSELLS).contains(MID_TIER);
    }

    public Observable<Boolean> offlineContentEnabled() {
         return getUpdates(OFFLINE_CONTENT);
    }

    private Observable<Boolean> getUpdates(String name) {
        return featureStorage.getUpdates(name);
    }

}

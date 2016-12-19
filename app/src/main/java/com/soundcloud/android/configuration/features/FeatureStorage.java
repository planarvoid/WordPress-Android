package com.soundcloud.android.configuration.features;

import static com.soundcloud.android.configuration.ConfigurationManager.TAG;

import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;

public class FeatureStorage {

    private static final String ENABLED_POSTFIX = "_enabled";
    private static final String PLANS_POSTFIX = "_plans";

    private final SharedPreferences sharedPreferences;

    private final Func1<String, Boolean> toValue = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return sharedPreferences.getBoolean(key, false);
        }
    };

    @Inject
    public FeatureStorage(@Named(StorageModule.FEATURES) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void update(List<Feature> features) {
        for (Feature feature : features) {
            update(feature);
        }
    }

    public void update(Feature feature) {
        Log.d(TAG, "updating feature: " + feature);
        updateEnabled(feature.name, feature.enabled);
        updatePlans(feature.name, feature.plans);
    }

    private void updateEnabled(String key, boolean enabled) {
        sharedPreferences.edit().putBoolean(key + ENABLED_POSTFIX, enabled).apply();
    }

    private void updatePlans(String key, List<Plan> plans) {
        sharedPreferences.edit().putStringSet(key + PLANS_POSTFIX, Plan.toIds(plans)).apply();
    }

    public boolean isEnabled(String name, boolean defaultValue) {
        return sharedPreferences.getBoolean(name + ENABLED_POSTFIX, defaultValue);
    }

    public Observable<Boolean> getUpdates(final String name) {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                         .filter(isFeature(name))
                         .map(toValue);
    }

    private Func1<String, Boolean> isFeature(final String name) {
        return feature -> feature.equals(name + ENABLED_POSTFIX);
    }

    public List<Plan> getPlans(String name) {
        return Plan.fromIds(sharedPreferences.getStringSet(name + PLANS_POSTFIX, Collections.emptySet()));
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}

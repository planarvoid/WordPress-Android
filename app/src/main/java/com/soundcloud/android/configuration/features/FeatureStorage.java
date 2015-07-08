package com.soundcloud.android.configuration.features;

import com.google.common.collect.Lists;
import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.storage.StorageModule;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
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
        updateEnabled(feature.name, feature.enabled);
        updatePlans(feature.name, feature.plans);
    }

    private void updateEnabled(String key, boolean enabled) {
        sharedPreferences.edit().putBoolean(key + ENABLED_POSTFIX, enabled).apply();
    }

    private void updatePlans(String key, List<String> values) {
        sharedPreferences.edit().putStringSet(key + PLANS_POSTFIX, new HashSet<>(values)).apply();
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
        return new Func1<String, Boolean>() {
            @Override
            public Boolean call(String feature) {
                return feature.equals(name + ENABLED_POSTFIX);
            }
        };
    }

    public List<String> getPlans(String name) {
        return Lists.newArrayList(sharedPreferences.getStringSet(name + PLANS_POSTFIX, new HashSet<String>()));
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}

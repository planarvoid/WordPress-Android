package com.soundcloud.android.configuration.features;

import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

public class FeatureStorage {

    private final SharedPreferences sharedPreferences;

    private final Func1<String, Boolean> toValue = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return sharedPreferences.getBoolean(key, false);
        }
    };

    @Inject
    public FeatureStorage(@Named("Features") SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isEnabled(final String featureName, final boolean defaultValue) {
        return sharedPreferences.getBoolean(featureName, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Boolean> list() {
        return (Map<String, Boolean>) sharedPreferences.getAll();
    }

    public void update(Map<String, Boolean> features) {
        final SharedPreferences.Editor edit = sharedPreferences.edit();
        for (Map.Entry<String, Boolean> feature : features.entrySet()) {
            edit.putBoolean(feature.getKey(), feature.getValue());
        }
        edit.apply();
    }

    public void update(String name, boolean enabled) {
        sharedPreferences.edit().putBoolean(name, enabled).apply();
    }

    public Observable<Boolean> getUpdates(final String name) {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                .filter(isFeature(name))
                .map(toValue);
    }

    private Func1<String, Boolean> isFeature(final String name) {
        return new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
                return s.equals(name);
            }
        };
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}

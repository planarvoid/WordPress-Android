package com.soundcloud.android.configuration.features;

import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.storage.StorageModule;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

public class FeatureStorage {

    private final SharedPreferences sharedPreferences;
    private final Obfuscator obfuscator;

    private final Func1<String, Boolean> toValue = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return toBoolean(sharedPreferences.getString(key, null), false);
        }
    };

    @Inject
    public FeatureStorage(@Named(StorageModule.FEATURES) SharedPreferences sharedPreferences, Obfuscator obfuscator) {
        this.sharedPreferences = sharedPreferences;
        this.obfuscator = obfuscator;
    }

    public void update(String name, boolean enabled) {
        String key = obfuscator.obfuscate(name);
        String value = obfuscator.obfuscate(enabled);
        sharedPreferences.edit().putString(key, value).apply();
    }

    public boolean isEnabled(String name, boolean defaultValue) {
        String enabled = sharedPreferences.getString(obfuscator.obfuscate(name), null);
        return toBoolean(enabled, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Boolean> list() {
        Map<String, String> obfuscatedMap = (Map<String, String>) sharedPreferences.getAll();
        Map<String, Boolean> featureMap = new HashMap<>();

        for (Map.Entry<String, String> entry : obfuscatedMap.entrySet()) {
            String key = obfuscator.deobfuscateString(entry.getKey());
            boolean value = obfuscator.deobfuscateBoolean(entry.getValue());
            featureMap.put(key, value);
        }
        return featureMap;
    }

    public void update(Map<String, Boolean> features) {
        for (Map.Entry<String, Boolean> feature : features.entrySet()) {
            update(feature.getKey(), feature.getValue());
        }
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
                return obfuscator.deobfuscateString(s).equals(name);
            }
        };
    }

    private boolean toBoolean(String value, boolean defaultValue) {
        return value == null
                ? defaultValue
                : obfuscator.deobfuscateBoolean(value);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}

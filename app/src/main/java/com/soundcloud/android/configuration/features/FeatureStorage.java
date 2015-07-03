package com.soundcloud.android.configuration.features;

import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.storage.StorageModule;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeatureStorage {

    private static final String ENABLED_POSTFIX = "_enabled";
    private static final String PLANS_POSTFIX = "_plans";

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

    public void update(List<Feature> features) {
        for (Feature feature : features) {
            update(feature);
        }
    }

    public void update(Feature feature) {
        update(feature.name + ENABLED_POSTFIX, feature.enabled);
        update(feature.name + PLANS_POSTFIX, feature.plans);
    }

    private void update(String name, boolean enabled) {
        String key = obfuscator.obfuscate(name);
        String value = obfuscator.obfuscate(enabled);
        sharedPreferences.edit().putString(key, value).apply();
    }

    private void update(String name, List<String> values) {
        String key = obfuscator.obfuscate(name);
        Set<String> obfuscatedValues = new HashSet<>();
        for (String value : values) {
            obfuscatedValues.add(obfuscator.obfuscate(value));
        }
        sharedPreferences.edit().putStringSet(key, obfuscatedValues).apply();
    }

    public boolean isEnabled(String name, boolean defaultValue) {
        String enabled = sharedPreferences.getString(obfuscator.obfuscate(name + ENABLED_POSTFIX), null);
        return toBoolean(enabled, defaultValue);
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
                return obfuscator.deobfuscateString(s).equals(name + ENABLED_POSTFIX);
            }
        };
    }

    private boolean toBoolean(String value, boolean defaultValue) {
        return value == null
                ? defaultValue
                : obfuscator.deobfuscateBoolean(value);
    }

    public List<String> getPlans(String name) {
        List<String> values = new ArrayList<>();
        Set<String> obfuscatedValues = sharedPreferences.getStringSet(obfuscator.obfuscate(name + PLANS_POSTFIX), new HashSet<String>());
        for (String value : obfuscatedValues) {
            values.add(obfuscator.deobfuscateString(value));
        }
        return values;
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}

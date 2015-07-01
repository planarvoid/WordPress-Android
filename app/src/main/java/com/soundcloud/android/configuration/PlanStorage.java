package com.soundcloud.android.configuration;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@VisibleForTesting
public class PlanStorage {

    private final SharedPreferences sharedPreferences;
    private final Obfuscator obfuscator;

    @Inject
    public PlanStorage(@Named(StorageModule.FEATURES) SharedPreferences sharedPreferences, Obfuscator obfuscator) {
        this.sharedPreferences = sharedPreferences;
        this.obfuscator = obfuscator;
    }

    public void update(String name, String value) {
        String key = obfuscator.obfuscate(name);
        String obfuscated = obfuscator.obfuscate(value);
        sharedPreferences.edit().putString(key, obfuscated).apply();
    }

    public void update(String name, List<String> values) {
        String key = obfuscator.obfuscate(name);
        Set<String> obfuscatedValues = new HashSet<>();
        for (String value : values) {
            obfuscatedValues.add(obfuscator.obfuscate(value));
        }
        sharedPreferences.edit().putStringSet(key, obfuscatedValues).apply();
    }

    public String get(String name, String defaultValue) {
        String value = sharedPreferences.getString(obfuscator.obfuscate(name), defaultValue);
        if (!value.equals(defaultValue)) {
            value = obfuscator.deobfuscateString(value);
        }
        return value;
    }

    public List<String> getList(String name) {
        List<String> values = new ArrayList<>();
        Set<String> obfuscatedValues = sharedPreferences.getStringSet(obfuscator.obfuscate(name), new HashSet<String>());
        for (String value : obfuscatedValues) {
            values.add(obfuscator.deobfuscateString(value));
        }
        return values;
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}

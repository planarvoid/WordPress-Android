package com.soundcloud.android.configuration;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

@VisibleForTesting
public class PlanStorage {

    private final SharedPreferences sharedPreferences;
    private final Obfuscator obfuscator;

    @Inject
    public PlanStorage(@Named(StorageModule.FEATURES) SharedPreferences sharedPreferences, Obfuscator obfuscator) {
        this.sharedPreferences = sharedPreferences;
        this.obfuscator = obfuscator;
    }

    public void update(String name, String status) {
        String key = obfuscator.obfuscate(name);
        String value = obfuscator.obfuscate(status);
        sharedPreferences.edit().putString(key, value).apply();
    }

    public String get(String name, String defaultValue) {
        String value = sharedPreferences.getString(obfuscator.obfuscate(name), defaultValue);
        if (!value.equals(defaultValue)) {
            value = obfuscator.deobfuscateString(value);
        }
        return value;
    }

    public void remove(String name) {
        String key = obfuscator.obfuscate(name);
        sharedPreferences.edit().remove(key).apply();
    }

}

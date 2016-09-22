package com.soundcloud.android.storage;

import android.content.SharedPreferences;

public class PersistentStorage {

    private final SharedPreferences sharedPreferences;

    public PersistentStorage(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void persist(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public boolean getValue(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void remove(String key) {
        if (contains(key)) {
            sharedPreferences.edit().remove(key).apply();
        }
    }

    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}

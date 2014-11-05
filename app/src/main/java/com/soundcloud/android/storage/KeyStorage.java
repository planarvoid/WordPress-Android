package com.soundcloud.android.storage;

import android.content.SharedPreferences;
import android.util.Base64;

import javax.inject.Inject;

public class KeyStorage {

    private final SharedPreferences preferences;

    @Inject
    public KeyStorage(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public void put(String name, byte[] value) {
        final String encodedValue = encodeForPrefs(value);
        preferences.edit().putString(name, encodedValue).apply();
    }

    public byte[] get(String name, byte[] defaultValue) {
        final String encodedDefaultValue = encodeForPrefs(defaultValue);
        final String value = preferences.getString(name, encodedDefaultValue);
        return decodeFromPrefs(value);
    }

    public boolean contains(String name) {
        return preferences.contains(name);
    }

    public void delete(String name) {
        preferences.edit().remove(name).apply();
    }

    private byte[] decodeFromPrefs(String keyString) {
        return Base64.decode(keyString, Base64.DEFAULT);
    }

    private String encodeForPrefs(byte[] key) {
        return Base64.encodeToString(key, Base64.DEFAULT);
    }
}

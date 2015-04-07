package com.soundcloud.android.crypto;

import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;
import android.util.Base64;

import javax.inject.Inject;
import javax.inject.Named;

class KeyStorage {

    private final SharedPreferences preferences;
    private final String ENCODED_EMPTY_VALUE = encodeForPrefs(DeviceSecret.EMPTY.getKey());

    @Inject
    public KeyStorage(@Named(StorageModule.DEVICE_KEYS) SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public void put(DeviceSecret key) {
        final SharedPreferences.Editor editor = preferences.edit();
        final String encodedValue = encodeForPrefs(key.getKey());
        editor.putString(key.getName(), encodedValue);

        if (key.hasInitVector()) {
            final String encodedIV = encodeForPrefs(key.getInitVector());
            final String ivKeyName = getIvKeyName(key.getName());
            editor.putString(ivKeyName, encodedIV);
        }
        editor.apply();
    }

    public DeviceSecret get(String name) {
        if (preferences.contains(name)) {
            final byte[] key = decodeKeyFromPrefs(name);
            final byte[] iv = decodeKeyFromPrefs(getIvKeyName(name));
            return new DeviceSecret(name, key, iv);
        }
        return DeviceSecret.EMPTY;
    }

    private String getIvKeyName(String keyName) {
        return keyName + ".iv";
    }

    private byte[] decodeKeyFromPrefs(String name) {
        final String encoded = preferences.getString(name, ENCODED_EMPTY_VALUE);
        return decodeFromPrefs(encoded);
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

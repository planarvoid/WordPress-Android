package com.soundcloud.android.storage;

import com.soundcloud.android.crypto.SecureKey;

import android.content.SharedPreferences;
import android.util.Base64;

import javax.inject.Inject;
import javax.inject.Named;

public class KeyStorage {

    private final SharedPreferences preferences;
    private final String ENCODED_EMPTY_VALUE = encodeForPrefs(SecureKey.EMPTY.getBytes());

    @Inject
    public KeyStorage(@Named("DeviceKeys") SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public void put(SecureKey key) {
        final SharedPreferences.Editor editor = preferences.edit();
        final String encodedValue = encodeForPrefs(key.getBytes());
        editor.putString(key.getName(), encodedValue);

        if (key.hasInitVector()) {
            final String encodedIV = encodeForPrefs(key.getInitVector());
            final String ivKeyName = getIvKeyName(key.getName());
            editor.putString(ivKeyName, encodedIV);
        }
        editor.apply();
    }

    public SecureKey get(String name) {
        if (preferences.contains(name)) {
            final byte[] key = decodeKeyFromPrefs(name);
            final byte[] iv = decodeKeyFromPrefs(getIvKeyName(name));
            return new SecureKey(name, key, iv);
        }
        return SecureKey.EMPTY;
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

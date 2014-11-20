package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.CryptoAssertions.expectByteArraysToBeEqual;

import com.google.common.base.Charsets;
import com.soundcloud.android.crypto.SecureKey;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class KeyStorageTest {
    private KeyStorage keyStorage;

    private final SecureKey testKey = getTestKeyFromString("my key é", "a portuguese valuë, 123");
    private final SharedPreferences preferences = Robolectric.application.getSharedPreferences("test", Context.MODE_PRIVATE);

    @Before
    public void setUp() throws Exception {
        preferences.edit().clear().apply();
        keyStorage = new KeyStorage(preferences);
    }

    @Test
    public void setAndGetSecretKey() {
        keyStorage.put(testKey);

        final SecureKey returnedKey = keyStorage.get(testKey.getName());
        expect(returnedKey.getName()).toEqual(testKey.getName());

        expectByteArraysToBeEqual(returnedKey.getBytes(), testKey.getBytes());
        expect(returnedKey.hasInitVector()).toBeFalse();
    }

    @Test
    public void setAndGetSecretKeyWithIV() {
        SecureKey keyWithIV = getTestKeyWithIV("my key");
        keyStorage.put(keyWithIV);

        final SecureKey returnedKey = keyStorage.get(keyWithIV.getName());
        expect(returnedKey.getName()).toEqual(keyWithIV.getName());
        expectByteArraysToBeEqual(returnedKey.getBytes(), keyWithIV.getBytes());

        expect(returnedKey.hasInitVector()).toBeTrue();
        expectByteArraysToBeEqual(returnedKey.getInitVector(), keyWithIV.getInitVector());
    }

    @Test
    public void getEmptyKeyForUnknown() {
        SecureKey key = keyStorage.get("unknown key");
        expect(key).toEqual(SecureKey.EMPTY);
    }

    @Test
    public void containsReturnTrueWhenTheValueExists() {
        keyStorage.put(testKey);
        expect(keyStorage.contains(testKey.getName())).toBeTrue();
    }

    @Test
    public void existReturnFalseWhenTheValueExists() {
        expect(keyStorage.contains("unknown key")).toBeFalse();
    }

    @Test
    public void deleteKey() {
        keyStorage.put(testKey);

        keyStorage.delete(testKey.getName());
        expect(keyStorage.contains(testKey.getName())).toBeFalse();
    }

    private SecureKey getTestKeyFromString(String name, String key) {
        return new SecureKey(name, key.getBytes(Charsets.UTF_8));
    }

    private SecureKey getTestKeyWithIV(String name) {
        return new SecureKey(name, "SomeKey".getBytes(Charsets.UTF_8), "ivbytes".getBytes(Charsets.UTF_8));
    }
}
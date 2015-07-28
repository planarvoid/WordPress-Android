package com.soundcloud.android.crypto;

import static com.soundcloud.android.testsupport.CryptoAssertions.expectByteArraysToBeEqual;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.strings.Charsets;
import org.junit.Before;
import org.junit.Test;

import android.content.Context;
import android.content.SharedPreferences;

public class KeyStorageTest extends AndroidUnitTest {
    private KeyStorage keyStorage;

    private final DeviceSecret testKey = getTestKeyFromString("my key é", "a portuguese valuë, 123");
    private final SharedPreferences preferences = sharedPreferences("test", Context.MODE_PRIVATE);

    @Before
    public void setUp() throws Exception {
        preferences.edit().clear().apply();
        keyStorage = new KeyStorage(preferences);
    }

    @Test
    public void setAndGetSecretKey() {
        keyStorage.put(testKey);

        final DeviceSecret returnedKey = keyStorage.get(testKey.getName());
        assertThat(returnedKey.getName()).isEqualTo(testKey.getName());

        expectByteArraysToBeEqual(returnedKey.getKey(), testKey.getKey());
        assertThat(returnedKey.hasInitVector()).isFalse();
    }

    @Test
    public void setAndGetSecretKeyWithIV() {
        DeviceSecret keyWithIV = getTestKeyWithIV("my key");
        keyStorage.put(keyWithIV);

        final DeviceSecret returnedKey = keyStorage.get(keyWithIV.getName());
        assertThat(returnedKey.getName()).isEqualTo(keyWithIV.getName());
        expectByteArraysToBeEqual(returnedKey.getKey(), keyWithIV.getKey());

        assertThat(returnedKey.hasInitVector()).isTrue();
        expectByteArraysToBeEqual(returnedKey.getInitVector(), keyWithIV.getInitVector());
    }

    @Test
    public void getEmptyKeyForUnknown() {
        DeviceSecret key = keyStorage.get("unknown key");
        assertThat(key).isEqualTo(DeviceSecret.EMPTY);
    }

    @Test
    public void containsReturnTrueWhenTheValueExists() {
        keyStorage.put(testKey);
        assertThat(keyStorage.contains(testKey.getName())).isTrue();
    }

    @Test
    public void existReturnFalseWhenTheValueExists() {
        assertThat(keyStorage.contains("unknown key")).isFalse();
    }

    @Test
    public void deleteKey() {
        keyStorage.put(testKey);

        keyStorage.delete(testKey.getName());
        assertThat(keyStorage.contains(testKey.getName())).isFalse();
    }

    private DeviceSecret getTestKeyFromString(String name, String key) {
        return new DeviceSecret(name, key.getBytes(Charsets.UTF_8));
    }

    private DeviceSecret getTestKeyWithIV(String name) {
        return new DeviceSecret(name, "SomeKey".getBytes(Charsets.UTF_8), "ivbytes".getBytes(Charsets.UTF_8));
    }
}
package com.soundcloud.android.crypto;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.CryptoAssertions.expectByteArraysToBeEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.google.common.base.Charsets;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.schedulers.TestScheduler;

import android.content.Context;
import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class CryptoOperationsTest {
    private CryptoOperations operations;
    private KeyStorage storage;

    @Before
    public void setUp() throws Exception {
        SharedPreferences preferences = Robolectric.application.getSharedPreferences("test", Context.MODE_PRIVATE);
        preferences.edit().clear().apply();
        storage = new KeyStorage(preferences);
        operations = new CryptoOperations(storage, new TestScheduler());
    }

    @Test
    public void generateKeyWhenItDoesNotExist() {
        byte[] key1 = operations.getKeyOrGenerateAndStore("my key");
        storage.delete("my key");
        byte[] key2 = operations.getKeyOrGenerateAndStore("my key");

        assertThat(key1, is(not(key2)));
    }

    @Test
    public void doNotRegenerateKeyIfItExists() {
        byte[] key1 = operations.getKeyOrGenerateAndStore("my key");
        byte[] key2 = operations.getKeyOrGenerateAndStore("my key");

        assertThat(key1, is(key2));
    }

    @Test
    public void returnTheKeyWhenItExists() {
        DeviceSecret storedKey = new DeviceSecret("my key", "blablabla".getBytes(Charsets.US_ASCII));
        storage.put(storedKey);

        byte[] returnedKey = operations.getKeyOrGenerateAndStore(storedKey.getName());

        expectByteArraysToBeEqual(returnedKey, storedKey.getKey());
    }

    @Test
    public void keyLengthShouldBe16() {
        expect(operations.getKeyOrGenerateAndStore("my key").length).toBe(16);
    }
}
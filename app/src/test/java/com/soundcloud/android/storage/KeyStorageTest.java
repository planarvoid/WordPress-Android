package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.base.Charsets;
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

    private final SharedPreferences preferences = Robolectric.application.getSharedPreferences("test", Context.MODE_PRIVATE);

    @Before
    public void setUp() throws Exception {
        preferences.edit().clear().apply();
        keyStorage = new KeyStorage(preferences);
    }

    @Test
    public void setAndGetValues() {
        byte[] insertedValue = getBytesFromString("a portuguese valuë, 123");
        keyStorage.put("my key é", insertedValue);

        byte[] returnedValue = keyStorage.get("my key é", getBytesFromString("default"));;
        assertThat(returnedValue, is(insertedValue));
    }

    @Test
    public void getDefaultValue() {
        byte[] defaultValue = getBytesFromString("default value");
        byte[] actual = keyStorage.get("unknown key", defaultValue);
        assertThat(actual, is(defaultValue));
    }

    @Test
    public void existReturnTrueWhenTheValueExists() {
        keyStorage.put("existing key", getBytesFromString("value"));
        expect(keyStorage.contains("existing key")).toBeTrue();
    }

    @Test
    public void existReturnFalseWhenTheValueExists() {
        expect(keyStorage.contains("unknown key")).toBeFalse();
    }

    @Test
    public void deleteKey() {
        keyStorage.put("key", getBytesFromString("value"));
        keyStorage.delete("key");
        expect(keyStorage.contains("key")).toBeFalse();
    }

    private byte[] getBytesFromString(String value) {
        return value.getBytes(Charsets.UTF_8);
    }
}
package com.soundcloud.android.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.storage.PersistentStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.content.SharedPreferences;

public class PersistentStorageTest extends AndroidUnitTest {
    private static final String KEY_ONE = "KeyOne";
    private static final String KEY_TWO = "KeyTwo";

    private PersistentStorage persistentStorage;
    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() {
        sharedPreferences = sharedPreferences();
        persistentStorage = new PersistentStorage(sharedPreferences);
    }

    @Test
    public void shouldSaveKeyToSharedPreferences() {
        persistentStorage.persist(KEY_ONE, true);
        persistentStorage.persist(KEY_TWO, false);

        assertThat(sharedPreferences.getBoolean(KEY_ONE, false)).isTrue();
        assertThat(sharedPreferences.getBoolean(KEY_TWO, true)).isFalse();
    }

    @Test
    public void shouldRemoveKeyFromSharedPreferences() {
        persistentStorage.persist(KEY_ONE, true);
        assertThat(sharedPreferences.getBoolean(KEY_ONE, false)).isTrue();

        persistentStorage.remove(KEY_ONE);
        assertThat(sharedPreferences.getBoolean(KEY_ONE, false)).isFalse();
    }

    @Test
    public void shouldGetValueFromSharedPreferences() {
        persistentStorage.persist(KEY_ONE, true);
        persistentStorage.persist(KEY_TWO, false);

        assertThat(persistentStorage.getValue(KEY_ONE, false)).isTrue();
        assertThat(persistentStorage.getValue(KEY_TWO, true)).isFalse();
    }
}

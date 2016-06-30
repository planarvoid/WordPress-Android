package com.soundcloud.android.gcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.fakes.RoboSharedPreferences;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public class GcmStorageTest extends AndroidUnitTest{

    private GcmStorage gcmStorage;

    @Mock private FeatureFlags featureFlags;

    @Before
    public void setUp() throws Exception {
        final HashMap<String, Map<String, Object>> prefsContent = new HashMap<>();
        final RoboSharedPreferences prefs = new RoboSharedPreferences(prefsContent, "prefs", Context.MODE_PRIVATE);
        gcmStorage = new GcmStorage(prefs, featureFlags);
    }

    @Test
    public void shouldRegisterIsTrueByDefault() {
        when(featureFlags.isEnabled(Flag.ARCHER_PUSH)).thenReturn(true);
        assertThat(gcmStorage.shouldRegister()).isTrue();
    }

    @Test
    public void shouldNotRegisterAfterMarkedAsRegistered() {
        when(featureFlags.isEnabled(Flag.ARCHER_PUSH)).thenReturn(true);
        gcmStorage.markAsRegistered("token");

        assertThat(gcmStorage.shouldRegister()).isFalse();
    }

    @Test
    public void hasTokenAfterMarkedAsRegistered() {
        when(featureFlags.isEnabled(Flag.ARCHER_PUSH)).thenReturn(true);
        gcmStorage.markAsRegistered("token");

        assertThat(gcmStorage.getToken()).isNotEmpty();
    }

    @Test
    public void shouldRegisterAfterClearingRegistration() {
        when(featureFlags.isEnabled(Flag.ARCHER_PUSH)).thenReturn(true);
        gcmStorage.markAsRegistered("token");
        gcmStorage.clearHasRegistered();

        assertThat(gcmStorage.shouldRegister()).isTrue();
    }

    @Test
    public void shouldNotHaveTokenAfterClearingRegisteration() {
        when(featureFlags.isEnabled(Flag.ARCHER_PUSH)).thenReturn(true);
        gcmStorage.markAsRegistered("token");
        gcmStorage.clearHasRegistered();

        assertThat(gcmStorage.getToken()).isNullOrEmpty();
    }

    @Test
    public void legacyShouldRegisterIsTrueByDefault() {
        assertThat(gcmStorage.shouldRegister()).isTrue();
    }

    @Test
    public void legacyShouldNotRegisterAfterMarkedAsRegistered() {
        gcmStorage.markAsRegistered("token");

        assertThat(gcmStorage.shouldRegister()).isFalse();
    }

    @Test
    public void legacyShouldRegisterAfterClearingRegistration() {
        gcmStorage.markAsRegistered("token");
        gcmStorage.clearHasRegistered();

        assertThat(gcmStorage.shouldRegister()).isTrue();
    }
}

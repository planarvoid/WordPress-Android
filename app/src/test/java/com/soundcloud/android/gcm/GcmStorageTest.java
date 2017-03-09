package com.soundcloud.android.gcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.SharedPreferences;

public class GcmStorageTest extends AndroidUnitTest{

    private GcmStorage gcmStorage;

    @Mock private ApplicationProperties applicationProperties;
    private SharedPreferences prefs;

    @Before
    public void setUp() throws Exception {
        prefs = sharedPreferences();
        gcmStorage = new GcmStorage(prefs, applicationProperties);
    }

    @Test
    public void shouldRegisterIsTrueByDefault() {
        when(applicationProperties.registerForGcm()).thenReturn(true);
        when(applicationProperties.registerForGcm()).thenReturn(true);
        assertThat(gcmStorage.shouldRegister()).isTrue();
    }

    @Test
    public void shouldNotRegisterAfterMarkedAsRegistered() {
        when(applicationProperties.registerForGcm()).thenReturn(true);
        gcmStorage.markAsRegistered("token");

        assertThat(gcmStorage.shouldRegister()).isFalse();
    }

    @Test
    public void hasTokenAfterMarkedAsRegistered() {
        when(applicationProperties.registerForGcm()).thenReturn(true);
        gcmStorage.markAsRegistered("token");

        assertThat(gcmStorage.getToken()).isNotEmpty();
    }

    @Test
    public void shouldRegisterArcherWithExistingToken() {
        when(applicationProperties.registerForGcm()).thenReturn(true);
        prefs.edit().putString(GcmStorage.TOKEN_KEY, "gcm-token");

        assertThat(gcmStorage.shouldRegister()).isTrue();
    }

    @Test
    public void shouldRegisterAfterClearingRegistration() {
        when(applicationProperties.registerForGcm()).thenReturn(true);
        gcmStorage.markAsRegistered("token");
        gcmStorage.clearTokenForRefresh();

        assertThat(gcmStorage.shouldRegister()).isTrue();
    }

    @Test
    public void shouldNotHaveTokenAfterClearingRegisteration() {
        when(applicationProperties.registerForGcm()).thenReturn(true);
        gcmStorage.markAsRegistered("token");
        gcmStorage.clearTokenForRefresh();

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
        gcmStorage.clearTokenForRefresh();

        assertThat(gcmStorage.shouldRegister()).isTrue();
    }
}

package com.soundcloud.android.gcm;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;

import java.io.IOException;
import java.util.Collections;

public class GcmRegistrationServiceTest extends AndroidUnitTest {

    private static final String TOKEN = "this-is-a-token";
    private static final ApiResponse OK_RESPONSE = TestApiResponses.ok();
    private static final ApiResponse FAILED_RESPONSE = TestApiResponses.networkError();

    private GcmRegistrationService service;

    @Mock private GcmStorage gcmStorage;
    @Mock private InstanceIdWrapper instanceId;
    @Mock private AppboyWrapper appboyWrapper;
    @Mock private ApiClient apiClient;
    @Mock private FeatureFlags featureFlags;

    private Intent intent = new Intent();

    @Before
    public void setUp() throws Exception {
        when(featureFlags.isDisabled(Flag.ARCHER_PUSH)).thenReturn(true);
        service = new GcmRegistrationService(gcmStorage, apiClient, instanceId, InjectionSupport.providerOf(appboyWrapper), featureFlags);
    }

    @Test
    public void doesNotFetchTokenIfStorageSaysNotTo() throws IOException {
        when(gcmStorage.shouldRegister()).thenReturn(false);

        service.onHandleIntent(intent);

        verify(instanceId, never()).getToken();
    }

    @Test
    public void storesSuccessfullyFetchedToken() throws IOException {
        when(gcmStorage.shouldRegister()).thenReturn(true);
        when(instanceId.getToken()).thenReturn(TOKEN);

        service.onHandleIntent(intent);

        verify(gcmStorage).markAsRegistered(TOKEN);
    }

    @Test
    public void sendsTokenToAppboyOnSuccessfullyFetchedToken() throws IOException {
        when(gcmStorage.shouldRegister()).thenReturn(true);
        when(instanceId.getToken()).thenReturn(TOKEN);

        service.onHandleIntent(intent);

        verify(appboyWrapper).handleRegistration(TOKEN);
    }

    @Test
    public void clearsTokenOnUnsuccessfullyFetchedToken() throws IOException {
        when(gcmStorage.shouldRegister()).thenReturn(true);
        when(instanceId.getToken()).thenReturn(null);

        service.onHandleIntent(intent);

        verify(gcmStorage).clearHasRegistered();
    }

    @Test
    public void marksRegisteredOnSuccessfullyFetchedToken() throws IOException {
        when(gcmStorage.shouldRegister()).thenReturn(true);
        when(featureFlags.isDisabled(Flag.ARCHER_PUSH)).thenReturn(false);
        when(instanceId.getToken()).thenReturn(TOKEN);
        when(apiClient.fetchResponse(argThat(isApiRequestTo("POST", "/push/register")
                .withContent(Collections.singletonMap("token", TOKEN))))).thenReturn(OK_RESPONSE);

        service.onHandleIntent(intent);

        verify(gcmStorage).markAsRegistered(TOKEN);
    }

    @Test
    public void doesNotMarksRegisteredOnUnsuccessfullyFetchedToken() throws IOException {
        when(gcmStorage.shouldRegister()).thenReturn(true);
        when(featureFlags.isDisabled(Flag.ARCHER_PUSH)).thenReturn(false);
        when(instanceId.getToken()).thenReturn(TOKEN);
        when(apiClient.fetchResponse(argThat(isApiRequestTo("POST", "/push/register")
                .withContent(Collections.singletonMap("token", TOKEN))))).thenReturn(FAILED_RESPONSE);

        service.onHandleIntent(intent);

        verify(gcmStorage, never()).markAsRegistered(anyString());
    }
}

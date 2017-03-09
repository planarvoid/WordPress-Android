package com.soundcloud.android.gcm;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
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
    @Mock private AccountOperations accountOperations;
    @Mock private ApplicationProperties applicationProperties;

    private Intent intent = new Intent();

    @Before
    public void setUp() throws Exception {
        when(applicationProperties.registerForGcm()).thenReturn(false);
        service = new GcmRegistrationService(gcmStorage,
                                             apiClient,
                                             instanceId,
                                             providerOf(appboyWrapper),
                                             featureFlags,
                                             accountOperations,
                                             applicationProperties);

        when(accountOperations.isUserLoggedIn()).thenReturn(true);
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
    public void marksRegisteredOnSuccessfullyFetchedToken() throws IOException {
        when(gcmStorage.shouldRegister()).thenReturn(true);
        when(applicationProperties.registerForGcm()).thenReturn(true);
        when(instanceId.getToken()).thenReturn(TOKEN);
        when(apiClient.fetchResponse(argThat(isApiRequestTo("POST", "/push/register")
                .withContent(Collections.singletonMap("token", TOKEN))))).thenReturn(OK_RESPONSE);

        service.onHandleIntent(intent);

        verify(gcmStorage).markAsRegistered(TOKEN);
    }

    @Test
    public void registersWithApiWithExistingToken() throws IOException {
        when(gcmStorage.shouldRegister()).thenReturn(true);
        when(gcmStorage.getToken()).thenReturn(TOKEN);
        when(applicationProperties.registerForGcm()).thenReturn(true);
        when(apiClient.fetchResponse(argThat(isApiRequestTo("POST", "/push/register")
                .withContent(Collections.singletonMap("token", TOKEN))))).thenReturn(OK_RESPONSE);

        service.onHandleIntent(intent);

        verify(instanceId, never()).getToken();
        verify(gcmStorage).markAsRegistered(TOKEN);
    }

    @Test
    public void doesNotRegisterTokenIfNotLoggedIn() throws IOException {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        when(gcmStorage.shouldRegister()).thenReturn(true);
        when(instanceId.getToken()).thenReturn(TOKEN);
        when(apiClient.fetchResponse(argThat(isApiRequestTo("POST", "/push/register")
                .withContent(Collections.singletonMap("token", TOKEN))))).thenReturn(OK_RESPONSE);

        service.onHandleIntent(intent);

        verify(apiClient, never()).fetchResponse(any(ApiRequest.class));
        verify(appboyWrapper, never()).handleRegistration(TOKEN);
    }

    @Test
    public void doesNotMarksRegisteredOnUnsuccessfullyFetchedToken() throws IOException {
        when(gcmStorage.shouldRegister()).thenReturn(true);
        when(applicationProperties.registerForGcm()).thenReturn(true);
        when(instanceId.getToken()).thenReturn(TOKEN);
        when(apiClient.fetchResponse(argThat(isApiRequestTo("POST", "/push/register")
                .withContent(Collections.singletonMap("token", TOKEN))))).thenReturn(FAILED_RESPONSE);

        service.onHandleIntent(intent);

        verify(gcmStorage, never()).markAsRegistered(anyString());
    }
}

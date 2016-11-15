package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.auth.GoogleAuthException;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.DeviceManagement;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;

import java.io.IOException;

public class GooglePlusSignInTaskTest extends AndroidUnitTest {

    private static final String ACCOUNT_NAME = "account name";
    private static final String SCOPE = "lulwatscope";

    @Mock private SoundCloudApplication app;
    @Mock private TokenInformationGenerator tokenInformationGenerator;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private Bundle bundle;
    @Mock private AccountOperations accountOperations;
    @Mock private ApiClient apiClient;
    @Mock private Token token;
    @Mock private ConfigurationOperations configurationOperations;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;

    private ApiUser user = ModelFixtures.create(ApiUser.class);

    private GooglePlusSignInTask task;

    @Before
    public void setUp() throws Exception {
        when(app.getAccountOperations()).thenReturn(accountOperations);
        when(tokenInformationGenerator.getToken(any(Bundle.class))).thenReturn(token);
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(true, false));
        task = new GooglePlusSignInTask(app, ACCOUNT_NAME, SCOPE, tokenInformationGenerator, storeUsersCommand,
                                        accountOperations, configurationOperations, new TestEventBus(), apiClient, syncInitiatorBridge);

        stub(tokenInformationGenerator.getGrantBundle(anyString(), anyString())).toReturn(bundle);
    }

    @Test
    public void shouldSpecifyTheCorrectGrantTypeWhenCreatingGrantString() throws IOException, GoogleAuthException {
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME), eq(SCOPE), any(Bundle.class))).thenReturn(
                "validtoken");
        task.doInBackground(bundle);
        verify(tokenInformationGenerator).getGrantBundle("urn:soundcloud:oauth2:grant-type:google_plus&access_token=",
                                                         "validtoken");
    }

    @Test
    public void shouldInvalidateTokenIfInvalidTokenExceptionIsThrown() throws IOException, GoogleAuthException, ApiRequestException {
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME), eq(SCOPE), any(Bundle.class))).thenReturn(
                "invalidtoken");
        when(tokenInformationGenerator.getToken(any(Bundle.class))).thenThrow(TokenRetrievalException.class);
        task.doInBackground(bundle);
        verify(accountOperations, times(2)).invalidateGoogleAccountToken("invalidtoken");
    }

    @Test
    public void shouldReturnFailureIfGoogleTokenCouldNotBeObtained() throws IOException, GoogleAuthException {
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME), eq(SCOPE), any(Bundle.class))).thenThrow(
                GoogleAuthException.class);
        assertThat(task.doInBackground(bundle).wasSuccess()).isFalse();
    }

    @Test
    public void shouldReturnSuccessIfGoogleSignInWasSuccessful() throws IOException, GoogleAuthException, ApiMapperException, ApiRequestException {
        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("GET", ApiEndpoints.ME.path())), isA(TypeToken.class)))
                .thenReturn(Me.create(user));
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME), eq(SCOPE), any(Bundle.class))).thenReturn(
                "validtoken");
        when(app.addUserAccountAndEnableSync(eq(user), any(Token.class), any(SignupVia.class))).thenReturn(true);
        assertThat(task.doInBackground(bundle).wasSuccess()).isTrue();
    }

    @Test
    public void shouldRequestSpecificVisibleActivities() throws Exception {
        Bundle expectedExtras = new Bundle();
        expectedExtras.putString(GooglePlusSignInTask.KEY_REQUEST_VISIBLE_ACTIVITIES,
                                 "http://schemas.google.com/AddActivity " +
                                         "http://schemas.google.com/CreateActivity " +
                                         "http://schemas.google.com/ListenActivity");

        task.doInBackground(bundle);

        verify(accountOperations).getGoogleAccountToken(ACCOUNT_NAME, SCOPE, expectedExtras);
    }

}

package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.DeviceManagement;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
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
import java.util.Collections;

public class LoginTaskTest extends AndroidUnitTest {

    @Mock private SoundCloudApplication application;
    @Mock private TokenInformationGenerator tokenInformationGenerator;
    @Mock private Token token;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private ConfigurationOperations configurationOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private ApiClient apiClient;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    private ApiUser user = ModelFixtures.create(ApiUser.class);
    private Bundle bundle;

    private LoginTask loginTask;

    @Before
    public void setUp() throws Exception {
        bundle = new Bundle();
        loginTask = new LoginTask(application, tokenInformationGenerator, storeUsersCommand,
                                  configurationOperations, new TestEventBus(), accountOperations,
                                  apiClient, syncInitiatorBridge);

        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(true);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.ME.path())), isA(TypeToken.class)))
                .thenReturn(Me.create(user));
    }

    @Test
    public void shouldUpdateTokenOnAccountOperations() throws Exception {
        setupMocksToReturnToken();
        loginTask.doInBackground(bundle);
        verify(accountOperations).updateToken(token);
    }

    @Test
    public void shouldRequestTokenBasedOnBundleContents() throws Exception {
        loginTask.doInBackground(bundle);
        verify(tokenInformationGenerator).getToken(bundle);
    }

    @Test
    public void shouldStoreCurrentUser() throws Exception {
        setupMocksToReturnToken();
        loginTask.doInBackground(bundle);
        verify(storeUsersCommand).call(Collections.singleton(user));
    }

    @Test
    public void shouldReturnAuthenticationFailureIfUserPersistenceFails() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class), any(TypeToken.class))).thenThrow(new IOException());
        LegacyAuthTaskResult result = loginTask.doInBackground(bundle);
        assertThat(result.wasSuccess()).isFalse();
    }

    @Test
    public void shouldSpecifyOriginOfTokenIfTokenIsFromAPI() throws Exception {
        setupMocksToReturnToken();
        when(token.getSignup()).thenReturn("api");
        loginTask.doInBackground(bundle);
        verify(application).addUserAccountAndEnableSync(user, token, SignupVia.API);
    }

    @Test
    public void shouldNotSpecifySignupOriginIfTokenHasNoOriginString() throws Exception {
        setupMocksToReturnToken();
        when(token.getSignup()).thenReturn(null);
        loginTask.doInBackground(bundle);
        verify(application).addUserAccountAndEnableSync(user, token, SignupVia.NONE);
    }

    @Test
    public void shouldReturnFailureResultIfAddingAccountFails() throws Exception {
        setupMocksToReturnToken();
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(false);
        assertThat(loginTask.doInBackground(bundle).wasSuccess()).isFalse();

    }

    @Test
    public void shouldReturnSuccessResultIfAddingAccountSucceeds() throws Exception {
        setupMocksToReturnToken();
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(true);
        assertThat(loginTask.doInBackground(bundle).wasSuccess()).isTrue();
    }

    @Test
    public void shouldSendConfigurationUpdateRequest() throws Exception {
        setupMocksToReturnToken();
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(true);
        loginTask.doInBackground(bundle);

        verify(configurationOperations).registerDevice(token);
    }

    @Test
    public void unauthorizedRecoverableDeviceBlockResultsInDeviceConflictFailure() throws Exception {
        setupMocksToReturnToken();
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(false, true));

        LegacyAuthTaskResult result = loginTask.doInBackground(bundle);

        assertThat(result.wasDeviceConflict()).isTrue();
        assertThat(result.getLoginBundle()).isSameAs(bundle);
        assertThat(result.getLoginBundle().getBoolean(LoginTask.IS_CONFLICTING_DEVICE)).isTrue();
    }

    @Test
    public void unauthorizedUnrecoverableDeviceBlockResultsInDeviceBlockFailure() throws Exception {
        setupMocksToReturnToken();
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(false, false));

        LegacyAuthTaskResult result = loginTask.doInBackground(bundle);

        assertThat(result.wasSuccess()).isFalse();
        assertThat(result.wasDeviceBlock()).isTrue();
    }

    @Test
    public void failureToForceRegisterDeviceReturnsGenericError() throws Exception {
        setupMocksToReturnToken();
        when(configurationOperations.forceRegisterDevice(token)).thenReturn(new DeviceManagement(false, true));
        bundle.putBoolean(LoginTask.IS_CONFLICTING_DEVICE, true);

        LegacyAuthTaskResult result = loginTask.doInBackground(bundle);

        assertThat(result.wasSuccess()).isFalse();
        assertThat(result.getException()).isInstanceOf(AuthTaskException.class);
    }

    @Test
    public void successfulForceRegisterDeviceContinuesLogin() throws Exception {
        setupMocksToReturnToken();
        when(configurationOperations.forceRegisterDevice(token)).thenReturn(new DeviceManagement(true, false));
        bundle.putBoolean(LoginTask.IS_CONFLICTING_DEVICE, true);
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(true);

        assertThat(loginTask.doInBackground(bundle).wasSuccess()).isTrue();
    }

    private void setupMocksToReturnToken() throws Exception {
        when(tokenInformationGenerator.getToken(bundle)).thenReturn(token);
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(true, false));
    }

}

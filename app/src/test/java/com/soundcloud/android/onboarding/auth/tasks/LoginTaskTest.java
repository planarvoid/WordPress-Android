package com.soundcloud.android.onboarding.auth.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.FetchMeCommand;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.DeviceManagement;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;

public class LoginTaskTest extends AndroidUnitTest {

    @Mock private SoundCloudApplication application;
    @Mock private TokenInformationGenerator tokenInformationGenerator;
    @Mock private FetchMeCommand fetchMeCommand;
    @Mock private Token token;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private ConfigurationOperations configurationOperations;
    @Mock private AccountOperations accountOperations;

    private ApiUser user = ModelFixtures.create(ApiUser.class);

    private LoginTask loginTask;
    private Bundle bundle;

    @Before
    public void setUp() throws Exception {
        bundle = new Bundle();
        loginTask = new LoginTask(application, tokenInformationGenerator, fetchMeCommand, storeUsersCommand,
                configurationOperations, new TestEventBus(), accountOperations);
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
    public void shouldMakeRequestToCurrentUser() throws Exception {
        setupMocksToReturnToken();
        loginTask.doInBackground(bundle);
        verify(fetchMeCommand).call(null);
    }

    @Test
    public void shouldReturnAuthenticationFailureIfUserPersistenceFails() throws Exception {
        when(fetchMeCommand.call(any(Void.class))).thenReturn(null);
        AuthTaskResult result = loginTask.doInBackground(bundle);
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

        AuthTaskResult result = loginTask.doInBackground(bundle);

        assertThat(result.wasDeviceConflict()).isTrue();
        assertThat(result.getLoginBundle()).isSameAs(bundle);
        assertThat(result.getLoginBundle().getBoolean(LoginTask.IS_CONFLICTING_DEVICE)).isTrue();
    }

    @Test
    public void unauthorizedUnrecoverableDeviceBlockResultsInDeviceBlockFailure() throws Exception {
        setupMocksToReturnToken();
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(false, false));

        AuthTaskResult result = loginTask.doInBackground(bundle);

        assertThat(result.wasSuccess()).isFalse();
        assertThat(result.wasDeviceBlock()).isTrue();
    }

    @Test
    public void failureToForceRegisterDeviceReturnsGenericError() throws Exception {
        setupMocksToReturnToken();
        when(configurationOperations.forceRegisterDevice(token)).thenReturn(new DeviceManagement(false, true));
        bundle.putBoolean(LoginTask.IS_CONFLICTING_DEVICE, true);

        AuthTaskResult result = loginTask.doInBackground(bundle);

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
        when(fetchMeCommand.call(any(Void.class))).thenReturn(Me.create(user));
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(true, false));
    }

}

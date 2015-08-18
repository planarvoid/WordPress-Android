package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.DeviceManagement;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.tasks.FetchUserTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class LoginTaskTest {
    private LoginTask loginTask;
    @Mock private SoundCloudApplication application;
    @Mock private TokenInformationGenerator tokenInformationGenerator;
    @Mock private FetchUserTask fetchUserTask;
    @Mock private Token token;
    @Mock private PublicApiUser user;
    @Mock private LegacyUserStorage userStorage;
    @Mock private ConfigurationOperations configurationOperations;
    @Mock private AccountOperations accountOperations;

    private Bundle bundle;

    @Before
    public void setUp() throws Exception {
        bundle = new Bundle();
        loginTask = new LoginTask(application, tokenInformationGenerator, fetchUserTask, userStorage,
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
        verify(fetchUserTask).currentUser();
    }

    @Test
    public void shouldReturnAuthenticationFailureIfUserPersistenceFails() throws Exception {
        when(fetchUserTask.currentUser()).thenReturn(null);
        AuthTaskResult result = loginTask.doInBackground(bundle);
        assertThat(result.wasSuccess(), is(false));
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
        assertThat(loginTask.doInBackground(bundle).wasSuccess(), is(false));

    }

    @Test
    public void shouldReturnSuccessResultIfAddingAccountSucceeds() throws Exception {
        setupMocksToReturnToken();
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(true);
        assertThat(loginTask.doInBackground(bundle).wasSuccess(), is(true));
    }

    @Test
    public void shouldSendConfigurationUpdateRequest() throws Exception {
        setupMocksToReturnToken();
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(true);
        loginTask.doInBackground(bundle);

        verify(configurationOperations).registerDevice(token);
    }

    @Test
    public void failureToAutorizeDeviceResultsInDeviceConflictFailure() throws Exception {
        setupMocksToReturnToken();
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(false, "device-id"));

        AuthTaskResult result = loginTask.doInBackground(bundle);

        expect(result.wasSuccess()).toBeFalse();
        expect(result.getLoginBundle()).toBe(bundle);
        expect(result.getLoginBundle().getString(LoginTask.CONFLICTING_DEVICE_KEY)).toEqual("device-id");
    }

    @Test
    public void failureToForceRegisterDeviceReturnsGenericError() throws Exception {
        setupMocksToReturnToken();
        String conflictingDeviceId = "conflicting";
        when(configurationOperations.forceRegisterDevice(token, conflictingDeviceId)).thenReturn(new DeviceManagement(false, "device-id"));
        bundle.putString(LoginTask.CONFLICTING_DEVICE_KEY, conflictingDeviceId);

        AuthTaskResult result = loginTask.doInBackground(bundle);

        expect(result.wasSuccess()).toBeFalse();
        expect(result.getException()).toBeInstanceOf(AuthTaskException.class);
    }

    @Test
    public void successfulForceRegisterDeviceContinuesLogin() throws Exception {
        setupMocksToReturnToken();
        String conflictingDeviceId = "conflicting";
        when(configurationOperations.forceRegisterDevice(token, conflictingDeviceId)).thenReturn(new DeviceManagement(true, null));
        bundle.putString(LoginTask.CONFLICTING_DEVICE_KEY, conflictingDeviceId);
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(true);

        assertThat(loginTask.doInBackground(bundle).wasSuccess(), is(true));
    }

    private void setupMocksToReturnToken() throws Exception {
        when(tokenInformationGenerator.getToken(bundle)).thenReturn(token);
        when(fetchUserTask.currentUser()).thenReturn(user);
        when(configurationOperations.registerDevice(token)).thenReturn(new DeviceManagement(true, "device-id"));
    }
}

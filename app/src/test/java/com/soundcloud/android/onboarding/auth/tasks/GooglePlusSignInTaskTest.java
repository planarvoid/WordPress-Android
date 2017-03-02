package com.soundcloud.android.onboarding.auth.tasks;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

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
import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.os.Bundle;

import java.io.IOException;

public class GooglePlusSignInTaskTest extends AndroidUnitTest {

    private static final String ACCOUNT_NAME = "account name";
    private static final String SCOPE = "lulwatscope";

    @Mock private SoundCloudApplication app;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private AccountOperations accountOperations;
    @Mock private ApiClient apiClient;
    @Mock private Token token;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private SignInOperations signInOperations;

    private final Bundle bundle = new Bundle();
    private ApiUser user = ModelFixtures.create(ApiUser.class);
    private Configuration configuration = ModelFixtures.create(Configuration.class);

    private GooglePlusSignInTask task;

    @Before
    public void setUp() throws Exception {
        task = new GooglePlusSignInTask(app, ACCOUNT_NAME, SCOPE, storeUsersCommand,
                                        accountOperations, syncInitiatorBridge,
                                        signInOperations);
    }

    @Test
    public void shouldTryToLoginWithTheCorrectParameters() throws IOException, GoogleAuthException {
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME), eq(SCOPE), any(Bundle.class))).thenReturn("validtoken");
        ArgumentCaptor<Bundle> bundleArgumentCaptor = ArgumentCaptor.forClass(Bundle.class);
        when(signInOperations.signIn(bundleArgumentCaptor.capture())).thenReturn(AuthTaskResult.success(new AuthResponse(new Token("validtoken", null), Me.create(user, configuration)), SignupVia.GOOGLE_PLUS));

        task.doInBackground(bundle);

        assertThat(bundleArgumentCaptor.getValue().containsKey(SignInOperations.GOOGLE_TOKEN_EXTRA));
    }

    @Test
    public void shouldInvalidateTokenIfInvalidTokenExceptionIsThrown() throws IOException, GoogleAuthException, ApiRequestException {
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME), eq(SCOPE), any(Bundle.class))).thenReturn("invalidtoken");
        when(signInOperations.signIn(any())).thenReturn(AuthTaskResult.failure("Error"));

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
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.ME.path())), isA(TypeToken.class))).thenReturn(Me.create(user, configuration));
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME), eq(SCOPE), any(Bundle.class))).thenReturn("validtoken");
        when(app.addUserAccountAndEnableSync(eq(user), any(Token.class), any(SignupVia.class))).thenReturn(true);
        when(signInOperations.signIn(any())).thenReturn(AuthTaskResult.success(new AuthResponse(token, Me.create(user, configuration)), SignupVia.GOOGLE_PLUS));

        assertThat(task.doInBackground(bundle).wasSuccess()).isTrue();
    }

    @Test
    public void shouldRequestSpecificVisibleActivities() throws Exception {
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME), eq(SCOPE), any(Bundle.class))).thenReturn("validtoken");
        task.doInBackground(bundle);

        ArgumentCaptor<Bundle> argumentCaptor = ArgumentCaptor.forClass(Bundle.class);

        verify(accountOperations).getGoogleAccountToken(eq(ACCOUNT_NAME), eq(SCOPE), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getString(GooglePlusSignInTask.KEY_REQUEST_VISIBLE_ACTIVITIES))
                .isEqualTo("http://schemas.google.com/AddActivity " +
                                   "http://schemas.google.com/CreateActivity " +
                                   "http://schemas.google.com/ListenActivity");

    }
}

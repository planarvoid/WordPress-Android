package com.soundcloud.android.task.auth;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.activity.auth.TokenInformationGenerator;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.os.Bundle;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class LoginTaskTest {
    private LoginTask loginTask;
    @Mock private SoundCloudApplication application;
    @Mock private TokenInformationGenerator tokenInformationGenerator;
    @Mock private Bundle bundle;
    @Mock private FetchUserTask fetchUserTask;
    @Mock private Token token;
    @Mock private User user;
    @Mock private UserStorage userStorage;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        loginTask = new LoginTask(application, tokenInformationGenerator, fetchUserTask, userStorage);
    }

    @Test
    public void shouldRequestTokenBasedOnBundleContents() throws Exception {
        when(tokenInformationGenerator.configureDefaultScopeExtra(bundle)).thenReturn(bundle);
        loginTask.doInBackground(bundle);
        verify(tokenInformationGenerator).getToken(bundle);
    }

    @Test
    public void shouldMakeRequestToMyDetailsEndpointToObtainUser(){
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        loginTask.doInBackground(bundle);
        verify(fetchUserTask).resolve(captor.capture());
        Request request = captor.getValue();

        assertThat(request.toUrl(), is(Endpoints.MY_DETAILS));
    }

    @Test
    public void shouldReturnAuthenticationFailureIfUserPersistenceFails() throws Exception {
        when(fetchUserTask.resolve(any(Request.class))).thenReturn(null);
        AuthTaskResult result = loginTask.doInBackground(bundle);
        assertThat(result.wasSuccess(), is(false));
    }

    @Test
    public void shouldSpecifyOriginOfTokenIfTokenIsFromAPI() throws IOException {
        setupMocksToReturnToken();
        when(token.getSignup()).thenReturn("api");
        loginTask.doInBackground(bundle);
        verify(application).addUserAccountAndEnableSync(user, token, SignupVia.API);
    }

    @Test
    public void shouldNotSpecifySignupOriginIfTokenHasNoOriginString() throws IOException{
        setupMocksToReturnToken();
        when(token.getSignup()).thenReturn(null);
        loginTask.doInBackground(bundle);
        verify(application).addUserAccountAndEnableSync(user, token, SignupVia.NONE);
    }


    @Test
    public void shouldReturnFailureResultIfAddingAccountFails() throws IOException {
        setupMocksToReturnToken();
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(false);
        assertThat(loginTask.doInBackground(bundle).wasSuccess(), is(false));

    }

    @Test
    public void shouldReturnSuccessResultIfAddingAccountSucceeds() throws IOException {
        setupMocksToReturnToken();
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.NONE)).thenReturn(true);
        assertThat(loginTask.doInBackground(bundle).wasSuccess(), is(true));

    }


    private void setupMocksToReturnToken() throws IOException {
        when(tokenInformationGenerator.configureDefaultScopeExtra(bundle)).thenReturn(bundle);
        when(tokenInformationGenerator.getToken(bundle)).thenReturn(token);
        when(fetchUserTask.resolve(any(Request.class))).thenReturn(user);
    }
}

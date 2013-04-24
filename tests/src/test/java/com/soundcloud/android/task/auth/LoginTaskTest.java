package com.soundcloud.android.task.auth;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.activity.auth.TokenUtil;
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
    @Mock
    private SoundCloudApplication application;
    @Mock
    private TokenUtil tokenUtil;
    @Mock
    private Bundle bundle;
    @Mock
    private FetchUserTask fetchUserTask;
    @Mock
    private Token token;
    @Mock
    private User user;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        loginTask = new LoginTask(application, tokenUtil, fetchUserTask);
    }

    @Test
    public void shouldRequestTokenBasedOnBundleContents() throws Exception {
        loginTask.doInBackground(bundle);
        verify(tokenUtil).getToken(application, bundle);
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
        when(tokenUtil.getToken(application, bundle)).thenReturn(token);
        when(application.getToken()).thenReturn(token);
        when(fetchUserTask.resolve(any(Request.class))).thenReturn(user);
        when(token.getSignup()).thenReturn("api");
        loginTask.doInBackground(bundle);
        verify(application).addUserAccountAndEnableSync(user, token, SignupVia.API);
    }

    @Test
    public void shouldNotSpecifySignupOriginIfTokenHasNoOriginString() throws IOException{
        when(tokenUtil.getToken(application, bundle)).thenReturn(token);
        when(application.getToken()).thenReturn(token);
        when(fetchUserTask.resolve(any(Request.class))).thenReturn(user);
        when(token.getSignup()).thenReturn(null);
        loginTask.doInBackground(bundle);
        verify(application).addUserAccountAndEnableSync(user, token, SignupVia.NONE);
    }
}

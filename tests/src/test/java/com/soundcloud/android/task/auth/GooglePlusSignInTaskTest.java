package com.soundcloud.android.task.auth;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.activity.auth.TokenInformationGenerator;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class GooglePlusSignInTaskTest {
    private static final String ACCOUNT_NAME = "account name";
    private static final String SCOPE = "lulwatscope";

    private GooglePlusSignInTask task;

    @Mock private SoundCloudApplication app;
    @Mock private TokenInformationGenerator tokenInformationGenerator;
    @Mock private FetchUserTask fetchUserTask;
    @Mock private UserStorage userStorage;
    @Mock private Bundle bundle;
    @Mock private AccountOperations accountOperations;
    @Mock private User user;
    @Mock private Token token;

    @Before
    public void setUp() {
        initMocks(this);
        task = new GooglePlusSignInTask(app, ACCOUNT_NAME, SCOPE, tokenInformationGenerator, fetchUserTask, userStorage, accountOperations);

        stub(tokenInformationGenerator.getGrantBundle(anyString(),anyString())).toReturn(bundle);
    }

    @Test
    public void shouldSpecifyTheCorrectGrantTypeWhenCreatingGrantString() throws IOException, GoogleAuthException {
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME),eq(SCOPE), any(Bundle.class))).thenReturn("validtoken");
        task.doInBackground(bundle);
        verify(tokenInformationGenerator).getGrantBundle("urn:soundcloud:oauth2:grant-type:google_plus&access_token=", "validtoken");
    }

    @Test
    public void shouldInvalidateTokenIfInvalidTokenExceptionIsThrown() throws IOException, GoogleAuthException {
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME),eq(SCOPE), any(Bundle.class))).thenReturn("validtoken");
        when(tokenInformationGenerator.getToken(any(AndroidCloudAPI.class), any(Bundle.class))).thenThrow(CloudAPI.InvalidTokenException.class);
        task.doInBackground(bundle);
        verify(accountOperations, times(2)).invalidateGoogleAccountToken("validtoken");
    }

    @Test
    public void shouldReturnFailureIfGoogleTokenCouldNotBeObtained() throws IOException, GoogleAuthException {
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME),eq(SCOPE), any(Bundle.class))).thenThrow(GoogleAuthException.class);
        assertThat(task.doInBackground(bundle).wasSuccess(), is(false));
    }

    @Test
    public void shouldReturnSuccessIfGoogleSignInWasSuccessful() throws IOException, GoogleAuthException {
        when(tokenInformationGenerator.getToken(eq(app), any(Bundle.class))).thenReturn(token);
        when(accountOperations.getGoogleAccountToken(eq(ACCOUNT_NAME),eq(SCOPE), any(Bundle.class))).thenReturn("validtoken");
        when(fetchUserTask.resolve(any(Request.class))).thenReturn(user);
        when(app.addUserAccountAndEnableSync(eq(user), any(Token.class), any(SignupVia.class))).thenReturn(true);
        assertThat(task.doInBackground(bundle).wasSuccess(), is(true));
    }

    @Test
    public void shouldRequestSpecificVisibleActivities() throws Exception {
        Bundle expectedExtras = new Bundle();
        expectedExtras.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES,  "http://schemas.google.com/AddActivity " +
                "http://schemas.google.com/CreateActivity " +
                "http://schemas.google.com/ListenActivity");

        task.doInBackground(bundle);

        verify(accountOperations).getGoogleAccountToken(ACCOUNT_NAME, SCOPE, expectedExtras);
    }

}

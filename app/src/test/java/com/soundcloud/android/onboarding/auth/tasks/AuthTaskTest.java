package com.soundcloud.android.onboarding.auth.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.storage.LegacyUserStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.os.Bundle;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class AuthTaskTest {

    @Mock private SoundCloudApplication application;
    @Mock private PublicApiUser user;
    @Mock private Token token;
    @Mock private LegacyUserStorage userStorage;

    private AuthTask authTask;

    @Before
    public void setUp() throws IOException {
        authTask = new AuthTask(application, userStorage) {
            @Override
            protected AuthTaskResult doInBackground(Bundle... params) {
                return null;
            }
        };
    }

    @Test
    public void shouldReturnFalseIfAddAccountFails() throws Exception {
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.API)).thenReturn(false);
        assertThat(authTask.addAccount(user, token, SignupVia.API)).isFalse();
    }

    @Test
    public void shouldReturnTrueIfAddAccountSucceeds() throws Exception {
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.API)).thenReturn(true);
        assertThat(authTask.addAccount(user, token, SignupVia.API)).isTrue();
    }

    @Test
    public void shouldCreateUserIfUserAccountAddedSuccessfully(){
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.API)).thenReturn(true);
        authTask.addAccount(user, token, SignupVia.API);
        verify(userStorage).createOrUpdate(user);
    }

}

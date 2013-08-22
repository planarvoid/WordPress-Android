package com.soundcloud.android.task.auth;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Token;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class AuthTaskTest {
    AuthTask authTask;

    @Mock private SoundCloudApplication application;
    @Mock private User user;
    @Mock private Token token;
    @Mock private UserStorage userStorage;

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
        assertThat(authTask.addAccount(user, token, SignupVia.API), is(false));
    }

    @Test
    public void shouldReturnTrueIfAddAccountSucceeds() throws Exception {
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.API)).thenReturn(true);
        assertThat(authTask.addAccount(user, token, SignupVia.API), is(true));
    }

    @Test
    public void shouldCreateUserIfUserAccountAddedSuccessfully(){
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.API)).thenReturn(true);
        authTask.addAccount(user, token, SignupVia.API);
        verify(userStorage).createOrUpdate(user);
    }
}

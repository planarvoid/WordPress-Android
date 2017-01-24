package com.soundcloud.android.onboarding.auth.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.os.Bundle;

import java.io.IOException;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class AuthTaskTest {

    @Mock private SoundCloudApplication application;
    @Mock private Token token;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;

    private ApiUser user = ModelFixtures.create(ApiUser.class);

    private AuthTask authTask;

    @Before
    public void setUp() throws IOException {
        authTask = new AuthTask(application, storeUsersCommand, syncInitiatorBridge) {
            @Override
            protected LegacyAuthTaskResult doInBackground(Bundle... params) {
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
    public void shouldCreateUserIfUserAccountAddedSuccessfully() {
        when(application.addUserAccountAndEnableSync(user, token, SignupVia.API)).thenReturn(true);
        authTask.addAccount(user, token, SignupVia.API);
        verify(storeUsersCommand).call(Collections.singleton(user));
    }

}

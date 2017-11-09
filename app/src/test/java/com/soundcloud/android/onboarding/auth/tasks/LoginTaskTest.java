package com.soundcloud.android.onboarding.auth.tasks;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.os.Bundle;

@RunWith(MockitoJUnitRunner.class)
public class LoginTaskTest {

    @Mock private SoundCloudApplication application;
    @Mock private Token token;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private AccountOperations accountOperations;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private SignInOperations signInOperations;
    private ApiUser user = UserFixtures.apiUser();
    private Configuration configuration = ModelFixtures.create(Configuration.class);
    private Bundle bundle;

    private LoginTask loginTask;

    @Before
    public void setUp() throws Exception {
        bundle = new Bundle();
        loginTask = new LoginTask(application,
                                  storeUsersCommand,
                                  accountOperations,
                                  syncInitiatorBridge,
                                  signInOperations);
    }

    @Test
    public void callsOperationsWhenLoginCalled() throws Exception {
        when(signInOperations.signIn(eq(bundle))).thenReturn(AuthTaskResult.success(new AuthResponse(token, Me.createFromUserRecord(user, configuration, false)), SignupVia.API));
        loginTask.doInBackground(bundle);
        verify(signInOperations).signIn(eq(bundle));
    }
}

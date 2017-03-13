package com.soundcloud.android.onboarding.auth.tasks;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.Me;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.configuration.Configuration;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.onboarding.auth.SignUpOperations;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;

public class SignupTaskTest extends AndroidUnitTest {

    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private SoundCloudApplication application;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private SignUpOperations signUpOperations;
    @Mock private ConfigurationOperations configurationOperations;

    private SignupTask signupTask;

    @Before
    public void setUp() throws Exception {
        signupTask = new SignupTask(application,
                                    storeUsersCommand,
                                    syncInitiatorBridge,
                                    signUpOperations);
    }

    @Test
    public void forwardToOperationsWhenFeatureFlagEnabled() throws Exception {
        Token token = Token.EMPTY;
        Me me = Me.create(ModelFixtures.create(ApiUser.class), ModelFixtures.create(Configuration.class));
        Bundle bundle = getParamsBundle();

        when(signUpOperations.signUp(bundle)).thenReturn(AuthTaskResult.success(new AuthResponse(token, me), SignupVia.API));

        signupTask.doInBackground(bundle);
        verify(signUpOperations).signUp(bundle);
        verifyZeroInteractions(configurationOperations);
    }

    private Bundle getParamsBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(SignUpOperations.KEY_USERNAME, "username");
        bundle.putString(SignUpOperations.KEY_PASSWORD, "password");
        bundle.putSerializable(SignUpOperations.KEY_BIRTHDAY, BirthdayInfo.buildFrom(22));
        bundle.putString(SignUpOperations.KEY_GENDER, "fluid");
        return bundle;
    }

}

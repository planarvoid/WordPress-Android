package com.soundcloud.android.onboarding;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.utils.BugReporter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;


@RunWith(SoundCloudTestRunner.class)
public class OnboardActivityTest {

    private OnboardActivity activity;

    @Mock private AccountAuthenticatorResponse accountAuthenticatorResponse;
    @Mock private Intent intent;
    @Mock private Bundle bundle;
    @Mock private ConfigurationOperations configurationOperations;
    @Mock private BugReporter bugReporter;

    @Before
    public void setup() throws Exception {
        activity = new OnboardActivity(configurationOperations, bugReporter, new TestEventBus()) {
            @Override
            protected boolean wasAuthorizedViaSignupScreen() {
                return false;
            }
        };
    }

    @Test
    public void shouldSpecifyThatRequestHasContinueIfIntentContainsAuthenticatorResponse(){
        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        shadowOf(activity).setIntent(intent);
        activity.onCreate(null);
        verify(accountAuthenticatorResponse).onRequestContinued();
    }

    @Test
    public void shouldSpecifyResultOnAccountAuthenticationIfBundleIsNotNull(){
        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        shadowOf(activity).setIntent(intent);
        activity.onCreate(null);
        activity.setBundle(bundle);
        activity.finish();
        verify(accountAuthenticatorResponse).onResult(bundle);
    }

    @Test
    public void shouldSpecifyActionCancelledIfBundleIsNull(){
        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        shadowOf(activity).setIntent(intent);
        activity.onCreate(null);
        activity.finish();
        verify(accountAuthenticatorResponse).onError(anyInt(), anyString());
    }

}

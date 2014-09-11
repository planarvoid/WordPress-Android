package com.soundcloud.android.onboarding.auth;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;


@RunWith(SoundCloudTestRunner.class)
public class AbstractLoginActivityTest {

    private AbstractLoginActivity activity;

    @Mock private AccountAuthenticatorResponse accountAuthenticatorResponse;
    @Mock private Intent intent;
    @Mock private Bundle bundle;

    @Before
    public void setup() throws Exception {
        activity = new AbstractLoginActivity() {
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
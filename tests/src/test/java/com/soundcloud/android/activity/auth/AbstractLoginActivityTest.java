package com.soundcloud.android.activity.auth;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;


@RunWith(DefaultTestRunner.class)
public class AbstractLoginActivityTest {

    private AbstractLoginActivity abstractLoginActivity;

    @Mock private AccountAuthenticatorResponse accountAuthenticatorResponse;
    @Mock private Intent intent;
    @Mock private Bundle bundle;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        abstractLoginActivity = new AbstractLoginActivity() {};
    }

    @Test
    public void shouldSpecifyThatRequestHasContinueIfIntentContainsAuthenticatorResponse(){
        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        shadowOf(abstractLoginActivity).setIntent(intent);
        abstractLoginActivity.onCreate(null);
        verify(accountAuthenticatorResponse).onRequestContinued();
    }

    @Test
    public void shouldSpecifyResultOnAccountAuthenticationIfBundleIsNotNull(){
        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        shadowOf(abstractLoginActivity).setIntent(intent);
        abstractLoginActivity.onCreate(null);
        abstractLoginActivity.setBundle(bundle);
        abstractLoginActivity.finish();
        verify(accountAuthenticatorResponse).onResult(bundle);
    }

    @Test
    public void shouldSpecifyActionCancelledIfBundleIsNull(){
        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        shadowOf(abstractLoginActivity).setIntent(intent);
        abstractLoginActivity.onCreate(null);
        abstractLoginActivity.finish();
        verify(accountAuthenticatorResponse).onError(anyInt(), anyString());
    }

}
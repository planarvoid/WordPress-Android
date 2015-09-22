package com.soundcloud.android.onboarding;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.rx.eventbus.TestEventBus;
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

    private OnboardActivityWithSettableBundle activity;

    @Mock private AccountAuthenticatorResponse accountAuthenticatorResponse;
    @Mock private Intent intent;
    @Mock private Bundle bundle;
    @Mock private ConfigurationManager configurationManager;
    @Mock private BugReporter bugReporter;
    @Mock private TokenInformationGenerator tokenUtils;
    @Mock private Navigator navigator;
    @Mock private LoginManager facebookLoginManager;
    @Mock private CallbackManager facebookCallbackManager;

    @Before
    public void setup() throws Exception {
        activity = new OnboardActivityWithSettableBundle(configurationManager, bugReporter, new TestEventBus(),
                tokenUtils, navigator, new FacebookSdk(), facebookLoginManager, facebookCallbackManager) {
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

    private class OnboardActivityWithSettableBundle extends OnboardActivity {
        public OnboardActivityWithSettableBundle(
                ConfigurationManager configurationManager,
                BugReporter bugReporter,
                TestEventBus testEventBus,
                TokenInformationGenerator tokenUtils,
                Navigator navigator,
                FacebookSdk facebookSdk,
                LoginManager facebookLoginManager,
                CallbackManager facebookCallbackManager)
        {
            super(configurationManager,
                    bugReporter,
                    testEventBus,
                    tokenUtils,
                    navigator,
                    facebookSdk,
                    facebookLoginManager,
                    facebookCallbackManager);
        }

        protected void setBundle(Bundle bundle) {
            this.resultBundle = bundle;
        }
    }
}

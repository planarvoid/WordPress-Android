package com.soundcloud.android.onboarding;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.testsupport.TestActivityController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;

public class OnboardActivityTest extends AndroidUnitTest {

    private OnboardActivityWithSettableBundle activity;

    @Mock private AccountAuthenticatorResponse accountAuthenticatorResponse;
    @Mock private Intent intent;
    @Mock private ConfigurationManager configurationManager;
    @Mock private BugReporter bugReporter;
    @Mock private TokenInformationGenerator tokenUtils;
    @Mock private Navigator navigator;
    @Mock private LoginManager facebookLoginManager;
    @Mock private CallbackManager facebookCallbackManager;

    private TestEventBus eventBus = new TestEventBus();
    private FacebookSdk facebookSdk = new FacebookSdk();

    private TestActivityController activityController;

    @Before
    public void setup() throws Exception {
        activity = new OnboardActivityWithSettableBundle(configurationManager,
                                                         bugReporter,
                                                         eventBus,
                                                         tokenUtils,
                                                         navigator,
                                                         facebookSdk,
                                                         facebookLoginManager,
                                                         facebookCallbackManager);
        activityController = TestActivityController.of(activity);
    }

    @Test
    public void shouldSpecifyThatRequestHasContinueIfIntentContainsAuthenticatorResponse() {
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);

        activityController.setIntent(intent);
        activityController.create();

        verify(accountAuthenticatorResponse).onRequestContinued();
    }

    @Test
    public void shouldSpecifyResultOnAccountAuthenticationIfBundleIsNotNull() {
        final Bundle bundle = new Bundle();
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        activity.setBundle(bundle);

        activityController.setIntent(intent);
        activityController.create();
        activityController.finish();

        verify(accountAuthenticatorResponse).onResult(bundle);
    }

    @Test
    public void shouldSpecifyActionCancelledIfBundleIsNull() {
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);

        activityController.setIntent(intent);
        activityController.create(null);
        activityController.finish();

        verify(accountAuthenticatorResponse).onError(anyInt(), anyString());
    }

    private static class OnboardActivityWithSettableBundle extends OnboardActivity {

        public OnboardActivityWithSettableBundle(ConfigurationManager configurationManager,
                                                 BugReporter bugReporter,
                                                 EventBus eventBus,
                                                 TokenInformationGenerator tokenUtils,
                                                 Navigator navigator,
                                                 FacebookSdk facebookSdk,
                                                 LoginManager facebookLoginManager,
                                                 CallbackManager facebookCallbackManager) {
            super(configurationManager, bugReporter, eventBus, tokenUtils, navigator, facebookSdk,
                  facebookLoginManager, facebookCallbackManager);
        }

        protected void setBundle(Bundle bundle) {
            this.resultBundle = bundle;
        }

    }
}

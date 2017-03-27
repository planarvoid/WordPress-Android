package com.soundcloud.android.onboarding;

import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.onboarding.auth.GooglePlusSignInTaskFragment;
import com.soundcloud.android.onboarding.auth.LoginTaskFragment;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestActivityController;
import com.soundcloud.android.utils.BugReporter;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class OnboardActivityTest extends AndroidUnitTest {

    private OnboardActivityWithSettableBundle activity;

    @Mock private AccountAuthenticatorResponse accountAuthenticatorResponse;
    @Mock private Intent intent;
    @Mock private ConfigurationManager configurationManager;
    @Mock private BugReporter bugReporter;
    @Mock private Navigator navigator;
    @Mock private LoginManager facebookLoginManager;
    @Mock private CallbackManager facebookCallbackManager;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Mock private GooglePlusSignInTaskFragment.Factory googlePlusSignInTaskFragmentFactory;
    @Mock private LoginTaskFragment.Factory loginTaskFragmentFactory;

    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private FacebookSdk facebookSdk = new FacebookSdk();

    private TestActivityController activityController;

    @Before
    public void setup() throws Exception {
        activity = new OnboardActivityWithSettableBundle(configurationManager,
                                                         bugReporter,
                                                         eventBus,
                                                         navigator,
                                                         facebookSdk,
                                                         facebookLoginManager,
                                                         facebookCallbackManager,
                                                         performanceMetricsEngine,
                                                         loginTaskFragmentFactory,
                                                         googlePlusSignInTaskFragmentFactory);

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

    @Test
    public void shouldStartMeasuringLoginTimeOnLogin() {

        givenMockLoginTaskFragment();
        givenMockGoooglePlusSignInTaskFragment();

        final Intent intent = new Intent();

        activityController.setIntent(intent);
        activityController.create(null);

        activity.onLogin("whatever", "whatever");

        verify(performanceMetricsEngine).startMeasuring(performanceMetricArgumentCaptor.capture());
        assertThat(performanceMetricArgumentCaptor.getValue())
                .hasMetricType(MetricType.LOGIN)
                .containsMetricParam(MetricKey.LOGIN_PROVIDER, LoginProvider.PASSWORD.toString());
    }

    @Test
    public void shouldStartMeasuringLoginTimeOnGooglePlusSignIn() {

        givenMockLoginTaskFragment();
        givenMockGoooglePlusSignInTaskFragment();

        final Intent intent = new Intent();

        activityController.setIntent(intent);
        activityController.create(null);

        activity.onActivityResult(Consts.RequestCodes.SIGNUP_VIA_GOOGLE, Activity.RESULT_OK, null);
        activity.onResumeFragments();

        verify(performanceMetricsEngine).startMeasuring(performanceMetricArgumentCaptor.capture());
        assertThat(performanceMetricArgumentCaptor.getValue())
                .hasMetricType(MetricType.LOGIN)
                .containsMetricParam(MetricKey.LOGIN_PROVIDER, LoginProvider.GOOGLE.toString());
    }

    @Test
    public void shouldStartMeasuringLoginTimeOnFacebookLogin() {

        givenMockLoginTaskFragment();
        givenMockGoooglePlusSignInTaskFragment();

        final Intent intent = new Intent();

        activityController.setIntent(intent);
        activityController.create(null);

        activity.loginWithFacebook("token");

        verify(performanceMetricsEngine).startMeasuring(performanceMetricArgumentCaptor.capture());
        assertThat(performanceMetricArgumentCaptor.getValue())
                .hasMetricType(MetricType.LOGIN)
                .containsMetricParam(MetricKey.LOGIN_PROVIDER, LoginProvider.FACEBOOK.toString());
    }

    private void givenMockGoooglePlusSignInTaskFragment() {
        when(googlePlusSignInTaskFragmentFactory.create(any(Bundle.class))).thenReturn(mock(GooglePlusSignInTaskFragment.class));
    }

    private void givenMockLoginTaskFragment() {
        when(loginTaskFragmentFactory.create(any(Bundle.class))).thenReturn(mock(LoginTaskFragment.class));
        when(loginTaskFragmentFactory.create(anyString(), anyString())).thenReturn(mock(LoginTaskFragment.class));
    }

    private static class OnboardActivityWithSettableBundle extends OnboardActivity {

        public OnboardActivityWithSettableBundle(ConfigurationManager configurationManager,
                                                 BugReporter bugReporter,
                                                 EventBus eventBus,
                                                 Navigator navigator,
                                                 FacebookSdk facebookSdk,
                                                 LoginManager facebookLoginManager,
                                                 CallbackManager facebookCallbackManager,
                                                 PerformanceMetricsEngine performanceMetricsEngine,
                                                 LoginTaskFragment.Factory loginTaskFragmentFactory,
                                                 GooglePlusSignInTaskFragment.Factory googlePlusSignInTaskFragmentFactory) {
            super(configurationManager, bugReporter, eventBus, navigator, facebookSdk,
                  facebookLoginManager, facebookCallbackManager, performanceMetricsEngine,
                  loginTaskFragmentFactory, googlePlusSignInTaskFragmentFactory);
        }

        protected void setBundle(Bundle bundle) {
            this.resultBundle = bundle;
        }

    }
}

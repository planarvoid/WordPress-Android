package com.soundcloud.android.tests.auth.login;

import static com.soundcloud.android.R.string.authentication_error_no_connection_message;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.SoundCloudTestApplication.fromContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.LoginErrorScreen;
import com.soundcloud.android.tests.SoundCloudTestApplication;
import com.soundcloud.android.tests.auth.LoginTest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import android.app.Activity;

public class OfflineFacebookLoginFlowTest extends LoginTest {

    private LoginManager facebookLoginManager;
    private HomeScreen homeScreen;
    @Captor private ArgumentCaptor<FacebookCallback<LoginResult>> facebookCallbackArgumentCaptor;

    @Override
    public void setUp() throws Exception {
        initMocks(this);
        super.setUp();

        homeScreen = new HomeScreen(solo);
        final SoundCloudTestApplication application = fromContext(getInstrumentation().getTargetContext());
        facebookLoginManager = application.getLoginManager();
        doAnswer(invocationOnMock -> {
            verify(facebookLoginManager).registerCallback(any(CallbackManager.class), facebookCallbackArgumentCaptor.capture());
            facebookCallbackArgumentCaptor.getValue().onError(new FacebookAuthorizationException("net::ERR_INTERNET_DISCONNECTED"));
            return null;
        }).when(facebookLoginManager).logInWithReadPermissions(any(Activity.class), anyCollection());

        connectionHelper.setNetworkConnected(false);
        stopWiremock(); // these depend on no connection
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        reset(facebookLoginManager);
    }

    @Test
    public void testLoginWithFacebookAccountWithoutNetworkConnection() throws Exception {
        LoginErrorScreen loginErrorScreen = homeScreen
                .clickLogInButton()
                .clickOnFBSignInButton()
                .failToLoginWithResult();

        assertThat(loginErrorScreen, is(visible()));
        assertThat(loginErrorScreen.errorMessage(),
                   is(solo.getString(authentication_error_no_connection_message)));
    }
}
